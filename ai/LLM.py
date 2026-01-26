import os
from typing import List, Optional
from fastapi import FastAPI
from pydantic import BaseModel
from langchain_community.llms import HuggingFacePipeline
from langchain.prompts import PromptTemplate
from langchain.chains import LLMChain
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline

# GPU 설정
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "2"

app = FastAPI()

# 로컬 LLM 로드 (메모리 효율을 위해 4-bit 양자화 권장)
# SSAFY 서버 사양에 맞춰 모델명은 조정 가능합니다. 
# 예: "MLP-KTLim/llama-3-Korean-Bllossom-8B"
model_id = "beomi/Llama-3-Open-Ko-8B" 
tokenizer = AutoTokenizer.from_pretrained(model_id)
model = AutoModelForCausalLM.from_pretrained(
    model_id,
    torch_dtype=torch.float16,
    device_map="auto"
)

pipe = pipeline(
    "text-generation",
    model=model,
    tokenizer=tokenizer,
    max_new_tokens=1024,
    temperature=0.7,
    top_p=0.9,
)

llm = HuggingFacePipeline(pipeline=pipe)

# 데이터 입력 스키마 정의 - 우선 예시로 둠
class NewsRequest(BaseModel):
    winning_team: str        # 승리 팀 (경찰/도둑)
    police_count: int       # 경찰 수
    thief_count: int        # 도둑 수
    time_info: str          # 시간대 (한밤중, 대낮 등)
    mvp: dict               # {"name": "이름", "record": "기록(횟수/시간)"}
    excellence: str         # 우수상 이름
    effort: str             # 노력상 이름

# LangChain 프롬프트 설정
template = """
당신은 긴박한 사건 현장을 생생하게 전달하는 사회부 기자입니다. 
제공된 게임 통계 데이터를 바탕으로 실제 뉴스 기사처럼 흥미진진한 리포트를 작성하세요.

[사건 데이터]
- 승리: {winning_team}
- 인원: 경찰 {police_count}명 vs 도둑 {thief_count}명
- 시간대: {time_info}
- MVP: {mvp_name} (기록: {mvp_record})
- 우수상: {excellence_name}
- 노력상: {effort_name}

[작성 가이드]
1. 뉴스 헤드라인을 눈에 띄게 작성할 것.
2. 기사 본문은 '오늘 {time_info}, ...'로 시작하여 긴장감 있게 서술할 것.
3. MVP의 활약상을 중심으로 작성하되, 우수상과 노력상 인물의 에피소드를 포함할 것.
4. 마지막은 사건의 종결이나 향후 전망으로 마무리할 것.

뉴스 기사:
"""

prompt = PromptTemplate(
    input_variables=["winning_team", "police_count", "thief_count", "time_info", "mvp_name", "mvp_record", "excellence_name", "effort_name"],
    template=template
)

news_chain = LLMChain(llm=llm, prompt=prompt)

@app.post("/generate-news")
async def generate_news(data: NewsRequest):
    # 체인 실행
    result = news_chain.run(
        winning_team=data.winning_team,
        police_count=data.police_count,
        thief_count=data.thief_count,
        time_info=data.time_info,
        mvp_name=data.mvp['name'],
        mvp_record=data.mvp['record'],
        excellence_name=data.excellence,
        effort_name=data.effort
    )
    
    return {"news_content": result.strip()}

if __name__ == "__main__":
    import uvicorn
    # 주피터 환경이므로 포트 번호 충돌에 주의하세요.
    uvicorn.run(app, host="0.0.0.0", port=8000)