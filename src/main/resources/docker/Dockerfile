# Use an official Python runtime as a parent image
FROM openjdk:8-jre-alpine

# Set the working directory to /app
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app
COPY .profile /root/
#Copy java policy to enable MBean Registration.
COPY java.policy /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/

COPY start.sh /app

# Install any needed packages specified in requirements.txt
#RUN pip install -r requirements.txt

# Make port 80 available to the world outside this container
#EXPOSE 9250

# Define environment variable
# ENV is set for ash to read profile.
ENV ENV .profile
ENV SHINIT .profile

# Run app.py when the container launches
RUN apk add vim
# CMD ["source", "/root/.profile"]
# CMD ["./start-registry.sh"]
# CMD ["python", "app.py"]

ENTRYPOINT /app/start.sh