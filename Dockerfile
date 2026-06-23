# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# Copy package files
COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml* ./

# Install all dependencies (devDependencies needed for build)
RUN pnpm install --frozen-lockfile

# Copy source code
COPY . .

# Run SvelteKit static build (paraglide-js compiled automatically via vite plugin)
RUN pnpm run build

# Production stage: lightweight nginx
FROM nginx:alpine AS runner

# Copy static build output
COPY --from=builder /app/build /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
