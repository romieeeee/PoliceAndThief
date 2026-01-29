package com.d104.pnt.data.source.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RegionCodeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 검색표 (Key: "시도 시군구", Value: "지역코드")
    private val regionMap = mutableMapOf<String, String>()

    private val hierarchyMap = mutableMapOf<String, MutableList<String>>()

    // 시/도 목록만 가져오기 (가나다순 정렬)
    val majorRegions: List<String>
        get() = hierarchyMap.keys.sorted()

    init {
        loadCsvData()
    }

    // 앱 켜질 때 딱 한 번 실행됨 (CSV 읽기)
    private fun loadCsvData() {
        try {
            // assets 폴더에서 파일 열기
            context.assets.open("region_codes.csv").bufferedReader().useLines { lines ->
                // drop(1) : 첫 번째 줄(헤더)은 무시하고, 두 번째 줄부터 실행
                lines.drop(1).forEach { line ->
                    val tokens = line.split(",")

                    val code = tokens[0].trim()
                    val major = tokens[1].trim()
                    val middle = tokens[2].trim()

                    val key = "$major $middle"
                    regionMap[key] = code

                    val list = hierarchyMap.getOrPut(shortenMajor(major)) { mutableListOf() }
                    list.add(middle)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun shortenMajor(major: String): String {
        val shortedMajor = when(major) {
            "서울특별시" -> "서울"
            "인천광역시" -> "인천"
            "강원특별자치도" -> "강원도"
            "경상북도" -> "경북"
            "경상남도" -> "경남"
            "전북특별자치도" -> "전북"
            "전라남도" -> "전남"
            "충청북도" -> "충북"
            "충청남도" -> "충남"
            "제주특별자치도" -> "제주도"
            "대구광역시" -> "대구"
            "부산광역시" -> "부산"
            "광주광역시" -> "광주"
            "대전광역시" -> "대전"
            "울산광역시" -> "울산"
            "세종특별자치시" -> "세종시"
            else -> major
        }
        return shortedMajor
    }

    // 외부에서 코드를 물어볼 때 쓰는 함수
    fun getRegionCode(major: String, middle: String): String? {
        // 입력받은 주소를 우리 키 형식("시도 시군구")으로 맞춰서 검색
        val key = "$major $middle"
        Timber.d(regionMap.toString())

        return regionMap[key]
    }

    // 시/도를 넣으면 시/군/구 리스트 반환
    fun getMiddleRegions(major: String): List<String> {
        return hierarchyMap[major]?.sorted() ?: emptyList()
    }
}