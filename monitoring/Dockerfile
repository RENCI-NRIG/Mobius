FROM python:3.7.3-stretch
MAINTAINER Komal Thareja<komal.thareja@gmail.com>
WORKDIR /code
VOLUME ["/code"]
ARG databasehost=database
ARG database=mobius
ARG user=mobius
ARG password=mobius
ARG mobiushost=mobius
ARG kafkahost
ARG tc=15
ARG td=85
ARG tm=85
ARG bucketcount=10
ARG leasedays=2 
ADD requirements.txt /code/
RUN pip install -r requirements.txt
COPY mobius.py /code/
COPY monitor_client.py /code/
COPY docker-entrypoint.sh /code/
CMD ["./docker-entrypoint.sh"]
