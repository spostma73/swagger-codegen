package io.swagger.codegen.languages;

import io.swagger.codegen.*;
import io.swagger.models.properties.*;

import java.util.*;
import java.io.File;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractTypeScriptClientCodegen extends DefaultCodegen implements CodegenConfig {

    protected String modelPropertyNaming= "camelCase";
    protected Boolean supportsES6 = true;

	public AbstractTypeScriptClientCodegen() {
	    super();
		supportsInheritance = true;
		setReservedWordsLowerCase(Arrays.asList(
                    // local variable names used in API methods (endpoints)
                    "varLocalPath", "queryParameters", "headerParams", "formParams", "useFormData", "varLocalDeferred",
                    "requestOptions",
                    // Typescript reserved words
                    "abstract", "await", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum", "export", "extends", "false", "final", "finally", "float", "for", "function", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this", "throw", "transient", "true", "try", "typeof", "var", "void", "volatile", "while", "with", "yield"));

		languageSpecificPrimitives = new HashSet<String>(Arrays.asList(
				"string",
				"String",
				"boolean",
				"Boolean",
				"Double",
				"Integer",
				"Long",
				"Float",
				"Object",
                "Array",
                "Date",
                "number",
                "any"
                ));
		instantiationTypes.put("array", "Array");

	    typeMapping = new HashMap<String, String>();
	    typeMapping.put("Array", "Array");
	    typeMapping.put("array", "Array");
	    typeMapping.put("List", "Array");
	    typeMapping.put("boolean", "boolean");
	    typeMapping.put("string", "string");
	    typeMapping.put("int", "number");
	    typeMapping.put("float", "number");
	    typeMapping.put("number", "number");
	    typeMapping.put("long", "number");
	    typeMapping.put("short", "number");
	    typeMapping.put("char", "string");
	    typeMapping.put("double", "number");
	    typeMapping.put("object", "any");
	    typeMapping.put("integer", "number");
	    typeMapping.put("Map", "any");
	    typeMapping.put("DateTime", "Date");
        //TODO binary should be mapped to byte array
        // mapped to String as a workaround
        typeMapping.put("binary", "string");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("UUID", "string");

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PROPERTY_NAMING, CodegenConstants.MODEL_PROPERTY_NAMING_DESC).defaultValue("camelCase"));
        cliOptions.add(new CliOption(CodegenConstants.SUPPORTS_ES6, CodegenConstants.SUPPORTS_ES6_DESC).defaultValue("false"));

	}

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.MODEL_PROPERTY_NAMING)) {
            setModelPropertyNaming((String) additionalProperties.get(CodegenConstants.MODEL_PROPERTY_NAMING));
        }

        if (additionalProperties.containsKey(CodegenConstants.SUPPORTS_ES6)) {
            setSupportsES6(Boolean.valueOf((String)additionalProperties.get(CodegenConstants.SUPPORTS_ES6)));
            additionalProperties.put("supportsES6", getSupportsES6());
        }
    }


	@Override
	public CodegenType getTag() {
	    return CodegenType.CLIENT;
	}

	@Override
	public String escapeReservedWord(String name) {
		return "_" + name;
	}

	@Override
	public String apiFileFolder() {
		return outputFolder + "/" + apiPackage().replace('.', File.separatorChar);
	}

	@Override
	public String modelFileFolder() {
		return outputFolder + "/" + modelPackage().replace('.', File.separatorChar);
	}

	@Override
	public String toParamName(String name) {
		// replace - with _ e.g. created-at => created_at
		name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

		// if it's all uppper case, do nothing
		if (name.matches("^[A-Z_]*$"))
			return name;

		// camelize the variable name
	        // pet_id => petId
		name = camelize(name, true);

		// for reserved word or word starting with number, append _
		if (isReservedWord(name) || name.matches("^\\d.*"))
			name = escapeReservedWord(name);

		return name;
	}

	@Override
	public String toVarName(String name) {
		// should be the same as variable name
		return getNameUsingModelPropertyNaming(name);
	}

	@Override
	public String toModelName(String name) {
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if (!StringUtils.isEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix;
        }

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            String modelName = camelize("model_" + name);
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            String modelName = camelize("model_" + name); // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

		// camelize the model name
		// phone_number => PhoneNumber
		return camelize(name);
	}

	@Override
	public String toModelFilename(String name) {
		// should be the same as the model name
		return toModelName(name);
	}

	@Override
	public String getTypeDeclaration(Property p) {
		if (p instanceof ArrayProperty) {
			ArrayProperty ap = (ArrayProperty) p;
			Property inner = ap.getItems();
			return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
		} else if (p instanceof MapProperty) {
			MapProperty mp = (MapProperty) p;
			Property inner = mp.getAdditionalProperties();
			return "{ [key: string]: "+ getTypeDeclaration(inner) + "; }";
		} else if (p instanceof FileProperty) {
			return "any";
		}
		return super.getTypeDeclaration(p);
	}

	@Override
	public String getSwaggerType(Property p) {
		String swaggerType = super.getSwaggerType(p);
		String type = null;
		if (typeMapping.containsKey(swaggerType)) {
			type = typeMapping.get(swaggerType);
			if (languageSpecificPrimitives.contains(type))
				return type;
		} else
			type = swaggerType;
		return toModelName(type);
	}

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        // append _ at the beginning, e.g. _return
        if (isReservedWord(operationId)) {
            return escapeReservedWord(camelize(sanitizeName(operationId), true));
        }

        return camelize(sanitizeName(operationId), true);
    }

    public void setModelPropertyNaming(String naming) {
        if ("original".equals(naming) || "camelCase".equals(naming) ||
            "PascalCase".equals(naming) || "snake_case".equals(naming)) {
            this.modelPropertyNaming = naming;
        } else {
            throw new IllegalArgumentException("Invalid model property naming '" +
              naming + "'. Must be 'original', 'camelCase', " +
              "'PascalCase' or 'snake_case'");
        }
    }

    public String getModelPropertyNaming() {
        return this.modelPropertyNaming;
    }

    public String getNameUsingModelPropertyNaming(String name) {
        switch (CodegenConstants.MODEL_PROPERTY_NAMING_TYPE.valueOf(getModelPropertyNaming())) {
            case original:    return name;
            case camelCase:   return camelize(name, true);
            case PascalCase:  return camelize(name);
            case snake_case:  return underscore(name);
            default:            throw new IllegalArgumentException("Invalid model property naming '" +
                                    name + "'. Must be 'original', 'camelCase', " +
                                    "'PascalCase' or 'snake_case'");
        }

    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            return value;
        } else {
            return "\'" + escapeText(value) + "\'";
        }
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        // number
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            String varName = new String(name);
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String enumName = sanitizeName(underscore(name).toUpperCase());
        enumName = enumName.replaceFirst("^_", "");
        enumName = enumName.replaceFirst("_$", "");

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String enumName = toModelName(property.name) + "Enum";

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    public void setSupportsES6(Boolean value) {
        supportsES6 = value;
    }

    public Boolean getSupportsES6() {
        return supportsES6;
    }
}
