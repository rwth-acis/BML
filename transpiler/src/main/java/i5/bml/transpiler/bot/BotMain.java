package i5.bml.transpiler.bot;

import java.io.IOException;
import java.net.URL;

public class BotMain {

    public static void main(String[] args) throws Exception {
        var url = BotMain.class.getClassLoader().getResource("swagger-codegen-cli-3.0.36.jar");
        assert url != null;

        generateOpenAPIClientCode(url, "https://petstore3.swagger.io/api/v3/openapi.json");

        new Bot().run(args[0]);
    }

    private static void generateOpenAPIClientCode(URL url, String openApiUrl) throws IOException {
        var apiName = openApiUrl.split("//")[1].split("\\.")[0];
        Runtime.getRuntime().exec(
                "/home/marc/.jdks/corretto-17.0.5/bin/java " +
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
                        "--api-package i5.bml.transpiler.bot.openapi.%sclient.apis ".formatted(apiName) +
                        "-i %s ".formatted(openApiUrl) +
                        "-l java " +
                        "--model-package i5.bml.transpiler.bot.openapi.%sclient.models ".formatted(apiName) +
                        "-o transpiler");
    }
}
