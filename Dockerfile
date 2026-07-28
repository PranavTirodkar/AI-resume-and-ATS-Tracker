# Build Stage for React + Vite Frontend
FROM node:20-alpine AS build-stage
WORKDIR /app

# Copy web dependencies and install
COPY web/package.json web/package-lock.json* ./web/
WORKDIR /app/web
RUN npm ci || npm install

# Copy web source and build
COPY web/ .
RUN npm run build

# Production Stage - Nginx
FROM nginx:alpine AS production-stage
COPY --from=build-stage /app/web/dist /usr/share/nginx/html

# Custom Nginx configuration for single page application routing
RUN echo 'server { \
    listen 8080; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
