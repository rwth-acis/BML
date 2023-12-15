package i5.bml.parser.types.components.services.databases;

import i5.bml.parser.types.*;

@BMLType(name = BuiltinType.MY_SQL, isComplex = true)
public class BMLMySQLComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "serverName", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String serverName;

    @BMLComponentParameter(name = "databaseName", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String databaseName;

    @BMLComponentParameter(name = "username", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String username;

    @BMLComponentParameter(name = "password", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String password;
}
