package walker;

import generatedParser.BMLBaseListener;
import generatedParser.BMLParser;
import org.apache.commons.validator.routines.UrlValidator;

public class UrlChecker extends BMLBaseListener {

    private void doUrlCheck(String url) {
        if (url.equals("url")) {
            url = url.substring(1, url.length() - 1);
            UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"},
                    UrlValidator.ALLOW_LOCAL_URLS + UrlValidator.ALLOW_ALL_SCHEMES);
            if (!urlValidator.isValid(url)) {
                throw new IllegalStateException("Url '%s' is not valid".formatted(url));
            }
        }
    }

    @Override
    public void exitElementValuePair(BMLParser.ElementValuePairContext ctx) {
        doUrlCheck(ctx.value.getText());
    }

    @Override
    public void exitElementExpressionPair(BMLParser.ElementExpressionPairContext ctx) {
        doUrlCheck(ctx.expr.getText());
    }
}
