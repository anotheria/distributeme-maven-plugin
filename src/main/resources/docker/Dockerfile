# Use an official Python runtime as a parent image
#FROM openjdk:21
FROM openjdk:21

# Set the working directory to /app
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app
COPY .profile /root/
#Copy java policy to enable MBean Registration (alpine)
COPY java.policy /usr/local/openjdk-21/lib/security/

#Copy java policy to enable MBean Registration open-jdk-21
COPY java.policy /usr/local/openjdk-21/conf/security


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
#Only for alpine:
#RUN apk add vim
#RUN apk add busybox-extras
# CMD ["source", "/root/.profile"]
# CMD ["./start-registry.sh"]
# CMD ["python", "app.py"]

ENTRYPOINT /app/start.sh
