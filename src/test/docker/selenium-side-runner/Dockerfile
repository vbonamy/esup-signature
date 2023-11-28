FROM node:21-alpine

RUN npm install -g selenium-side-runner

WORKDIR /root

CMD selenium-side-runner -s http://selenium-chrome-driver:4444 --output-directory /root/test-result /test-sides/*.side
