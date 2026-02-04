FROM node:24.12.0-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 8090 8091 8092 8093

CMD ["pm2-runtime", "start", "ecosystem.config.js"]