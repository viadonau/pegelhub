ARG BASE_IMAGE
FROM ${BASE_IMAGE}

COPY docker/java-entrypoint.sh /usr/local/bin/pegelhub-java-entrypoint

ENTRYPOINT ["/usr/local/bin/pegelhub-java-entrypoint"]
CMD ["java", "-version"]
