# syntax=docker/dockerfile:1.6

FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

FROM node:20-alpine AS runtime
ENV NODE_ENV=production
WORKDIR /app
USER node
COPY --chown=node:node --from=deps /app/node_modules ./node_modules
COPY --chown=node:node src ./src
EXPOSE 8080
CMD ["node", "src/app.js"]
