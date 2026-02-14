### Stage 1: Build ###
FROM node:22-alpine AS build
WORKDIR /app

# Copy dependency files first for caching
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Copy OpenAPI spec (from backend build artifact)
COPY backend/target/openapi.json /spec/openapi.json

# Copy source and generate API client
COPY frontend/ .
# Point generator at the spec location
RUN sed -i 's|../backend/target/openapi.json|/spec/openapi.json|' openapitools.json && \
    npm run generate-api

# Production build
RUN npx ng build --configuration=production

### Stage 2: Serve with nginx ###
FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
