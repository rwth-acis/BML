package i5.bml.transpiler.utils;

import java.io.IOException;

public class OpenAPIUtils {

    public static void generateOpenAPIClientCode(String openApiUrl, String outputPackage, String apiName, String botOutputPath) {
        var swaggerBinary = "swagger-codegen-cli-3.0.36.jar";
        var url = OpenAPIUtils.class.getClassLoader().getResource(swaggerBinary);
        if (url == null) {
            throw new IllegalStateException("Could not find %s".formatted(swaggerBinary));
        }

        var modifiedOutputPackage = outputPackage.isEmpty() ? "" : ".%s.".formatted(outputPackage);

        var command = System.getenv("JAVA_HOME") +
                " " +
                "-DhideGenerationTimestamp=false " +
                "-DsupportingFiles=ApiKeyAuth.java,Authentication.java,HttpBasicAuth.java,OAuth.java," +
                "OAuthFlow.java,ApiCallback.java,ApiClient.java,ApiException.java,ApiResponse.java," +
                "Configuration.java,GzipRequestInterceptor.java,JSON.java,Pair.java,ProgressRequestBody.java," +
                "ProgressResponseBody.java,StringUtil.java " +
                "-DmodelDocs=false " +
                "-DapiDocs=false " +
                "-Dapis " +
                "-DapiTests=false " +
                "-Dmodels " +
                "-DmodelTests=false " +
                "-jar %s ".formatted(url.getPath()) +
                "generate " +
                "--api-package %sopenapi.%s.apis ".formatted(modifiedOutputPackage, apiName) +
                "-i %s ".formatted(openApiUrl) +
                "-l java " +
                "--model-package %sopenapi.%s.models ".formatted(modifiedOutputPackage, apiName) +
                "-o %s".formatted(botOutputPath.replaceFirst("/src/main/java", ""));

        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
