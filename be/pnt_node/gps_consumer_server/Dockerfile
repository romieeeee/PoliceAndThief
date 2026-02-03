FROM node:24.12.0-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm install pm2 -g

CMD ["pm2-runtime", "start", "ecosystem.config.cjs"]
