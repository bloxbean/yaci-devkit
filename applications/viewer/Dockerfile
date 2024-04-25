FROM node:slim

RUN apt-get update && apt-get install -y \
  git \
  && rm -rf /var/lib/apt/lists/*

COPY . /yaci-viewer
WORKDIR /yaci-viewer

RUN npm install && npm run build

EXPOSE 5173

CMD ["npm", "run", "dev"]
