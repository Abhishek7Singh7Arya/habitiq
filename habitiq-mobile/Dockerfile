# Base image with Node.js
FROM node:18-alpine AS base

# Install system dependencies
RUN apk add --no-cache libc6-compat git bash

WORKDIR /app

# Copy package lock and configurations
COPY package*.json ./

# Install dependencies (use ci if possible or standard npm install)
RUN npm install

# Copy the rest of the application files
COPY . .

# Development stage for interactive Metro bundling and web support
FROM base AS development
ENV PORT=8081
ENV EXPO_DEVTOOLS_LISTEN_ADDRESS=0.0.0.0

# Expose default Metro port
EXPOSE 8081

# Command to run expo development server
CMD ["npx", "expo", "start", "--web"]

# Production Web builder stage
FROM base AS builder
# Export static web platform bundle
RUN npx expo export --platform web

# Production Web serving stage (using Nginx)
FROM nginx:alpine AS production-web
# Copy static files to nginx serving folder
COPY --from=builder /app/dist /usr/share/nginx/html
# Expose standard HTTP port
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
