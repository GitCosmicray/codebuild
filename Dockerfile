# Use an Nginx base image .
FROM nginx:alpine

# Copy website files to the default Nginx web root
COPY . /usr/share/nginx/html

# Expose port 80 for HTTP traffic
EXPOSE 8080

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]

