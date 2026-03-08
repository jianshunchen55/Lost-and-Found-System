FROM node:18
WORKDIR /app
COPY package.json /app/
COPY vue.config.js /app/
COPY tsconfig.json /app/
COPY src /app/src
COPY public /app/public
RUN npm install
EXPOSE 8081
CMD ["npm","run","serve"]
