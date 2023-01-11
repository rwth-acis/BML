package i5.bml.transpiler.utils;

import i5.bml.parser.utils.Measurements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class OpenAPIUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPIUtils.class);

    private OpenAPIUtils() {}

    public static void generateOpenAPIClientCode(String openAPISpec, String outputPackage, String apiName, String botOutputPath) {
        var swaggerBinary = "swagger-codegen-cli-3.0.36.jar";
        var url = OpenAPIUtils.class.getClassLoader().getResource(swaggerBinary);
        if (url == null) {
            LOGGER.error("Could not find {} in resources folder", swaggerBinary);
            return;
        }

        File specFile = null;
        try {
            specFile = File.createTempFile(apiName, "json");
            Files.write(specFile.toPath(), openAPISpec.getBytes());
        } catch (IOException e) {
            LOGGER.error("Failed to create temporary spec file");
            return;
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
                "-i %s ".formatted(specFile.getAbsolutePath()) +
                "-l java " +
                "--model-package %sopenapi.%s.models ".formatted(modifiedOutputPackage, apiName) +
                "-o %s".formatted(botOutputPath.replaceFirst("/src/main/java", ""));

        Measurements.measure("Swagger code generation", () -> {
            try {
                // We synchronize the generation to make sure that we do not start compilation without having all the
                // necessary code generated
                Runtime.getRuntime().exec(command).onExit().get();
            } catch (Exception e) {
                LOGGER.error("Failed to execute swagger code generation", e);
            }
        });
    }
}
