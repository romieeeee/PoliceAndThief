FROM node:24.12.0-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 8091

CMD ["npm", "run", "dev"]