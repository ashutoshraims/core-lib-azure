#Using Properties From Common.properties file of Core-Lib

Application properties that are common across all services are added in a separate file in core-lib called common.properties

1.  Set the property SPRING_CONFIG_NAME as "common,application" in the deployment.yaml file as shown below. This brings in all <b>common</b> properties from core-lib
    and also keeps the application properties specific for your service.
    ```
    - name: SPRING_CONFIG_NAME
      value: "common,application"
    ```