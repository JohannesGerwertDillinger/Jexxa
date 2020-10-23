package io.jexxa.infrastructure.drivingadapter.rest.openapi;


import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static io.jexxa.infrastructure.drivingadapter.rest.RESTfulRPCAdapter.OPEN_API_PATH;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.javalin.core.JavalinConfig;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation;
import io.jexxa.utils.JexxaLogger;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

@SuppressWarnings("java:S1602") // required to avoid ambiguous warnings
public class OpenAPIFacade
{
    private static final String APPLICATION_TYPE_JSON = "application/json";
    private static final String OBJET_TYPE_AS_JSON = "\"type\":\"object\"";
    private static final String ARRAY_TYPE_AS_JSON = "\"type\":\"array\"";

    private final Properties properties;
    private final JavalinConfig javalinConfig;
    private OpenApiOptions openApiOptions;

    public OpenAPIFacade(Properties properties, JavalinConfig javalinConfig)
    {
        this.properties = properties;
        this.javalinConfig = javalinConfig;
        initOpenAPI();
    }
    private void initOpenAPI()
    {
        if (properties.containsKey(OPEN_API_PATH))
        {
            Info applicationInfo = new Info()
                    .version("1.0")
                    .description(properties.getProperty("io.jexxa.context.name", "Unknown Context"))
                    .title(properties.getProperty("io.jexxa.context.name", "Unknown Context"));

            openApiOptions = new OpenApiOptions(applicationInfo)
                    .path("/" + properties.getProperty(OPEN_API_PATH));

            javalinConfig.registerPlugin(new OpenApiPlugin(openApiOptions));
            javalinConfig.enableCorsForAllOrigins();

            openApiOptions.defaultDocumentation(doc -> {
                doc.json("400", BadRequestResponse.class);
            });
        }
    }

    public void documentGET(Method method, String resourcePath)
    {
        if ( openApiOptions != null )
        {
            var openApiDocumentation = OpenApiBuilder
                    .document()
                    .operation(openApiOperation -> {
                        openApiOperation.operationId(method.getName());
                    });

            documentReturnType(method, openApiDocumentation);

            openApiOptions.setDocumentation(resourcePath, HttpMethod.GET, openApiDocumentation);
        }
    }

    public void documentPOST(Method method, String resourcePath)
    {
        if ( openApiOptions != null )
        {
            var openApiDocumentation = OpenApiBuilder
                    .document()
                    .operation(openApiOperation -> {
                        openApiOperation.operationId(method.getName());
                    });

            documentParameters(method, openApiDocumentation);

            documentReturnType(method, openApiDocumentation);

            openApiOptions.setDocumentation(resourcePath, HttpMethod.POST, openApiDocumentation);
        }
    }

    private static void documentReturnType(Method method, OpenApiDocumentation openApiDocumentation)
    {
        if ( isJsonArray(method.getReturnType()) )
        {
            openApiDocumentation.jsonArray("200", extractTypeFromArray(method.getGenericReturnType()));
        } else if ( method.getReturnType() != void.class )
        {
            openApiDocumentation.json("200", method.getReturnType());
        }
        else {
            openApiDocumentation.result("200");
        }
    }

    private static void documentParameters(Method method, OpenApiDocumentation openApiDocumentation)
    {
        if (method.getParameters().length == 1 )
        {
            var schema = createSchema(method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
            schema.setExample(createObject(method.getParameterTypes()[0], method.getGenericParameterTypes()[0]));
            openApiDocumentation.body(schema, APPLICATION_TYPE_JSON);
        }  else if ( method.getParameters().length > 1 )
        {
            var schema = new ComposedSchema();
            var exampleObjects = new Object[method.getParameterTypes().length];

            for (int i = 0; i < method.getParameterTypes().length; ++i)
            {
                exampleObjects[i] = createObject(method.getParameterTypes()[i],method.getGenericParameterTypes()[i]);
                var parameterSchema = createSchema(method.getParameterTypes()[i], method.getGenericParameterTypes()[i]);
                parameterSchema.setExample(exampleObjects[i]);

                openApiDocumentation.body(parameterSchema, APPLICATION_TYPE_JSON);
                schema.addAnyOfItem(parameterSchema);
            }

            schema.setExample(exampleObjects);
            openApiDocumentation.body(schema, APPLICATION_TYPE_JSON);
        }
    }

    private static String createSchemaAsString(Class<?> clazz) throws JsonProcessingException
    {
        var mapper = new ObjectMapper();
        //There are other configuration options you can set.  This is the one I needed.
        mapper.configure(WRITE_ENUMS_USING_TO_STRING, true);

        var schema = mapper.generateJsonSchema(clazz);


        return schema.toString();
    }

    private static Object createObject(Class<?> clazz, Type genericType)
    {
        try
        {
            var schemaString = createSchemaAsString(clazz);
            JsonElement element = JsonParser.parseString(schemaString);
            Gson gson = new Gson();
            if ( !schemaString.contains(OBJET_TYPE_AS_JSON) && !schemaString.contains(ARRAY_TYPE_AS_JSON) )
            {
                return createPrimitive(clazz);
            }
            if (!schemaString.contains(ARRAY_TYPE_AS_JSON))
            {
                return gson.fromJson(element, clazz);
            }
            if (schemaString.contains(ARRAY_TYPE_AS_JSON))
            {
                var result = new Object[1];
                result[0] = createObject(extractTypeFromArray(genericType), null);
                return result;
            }

        } catch (Exception e) {
            JexxaLogger.getLogger(OpenAPIFacade.class).warn( "Could not create Object {}" , clazz.getName() , e );
        }
        return null;
    }


    private static Object createPrimitive(Class<?> clazz)
    {
        if (isInteger(clazz) || isNumber(clazz))
        {
            return 1;
        }

        if (isBoolean( clazz))
        {
            return true;
        }

        if (isString(clazz))
        {
            return "string";
        }

        return null;
    }

    private static Schema<?> createSchema(Class<?> clazz, Type genericType)
    {
        if ( isInteger(clazz) )
        {
            return new IntegerSchema();
        }

        if ( isNumber(clazz) )
        {
            return new NumberSchema();
        }

        if ( isBoolean(clazz) )
        {
            return new BooleanSchema();
        }

        if ( isString(clazz) )
        {
            return new StringSchema();
        }

        if ( isJsonArray(clazz) )
        {
            var schema = new ArraySchema();
            schema.setItems(createSchema(extractTypeFromArray(genericType), null));
            return schema;
        }

        var result = new ObjectSchema();

        result.set$ref(clazz.getSimpleName());

        return result;
    }


    private static boolean isInteger(Class<?> clazz)
    {
        return clazz.equals(Short.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(short.class) ||
                clazz.equals(int.class);
    }

    private static boolean isNumber(Class<?> clazz)
    {
        return Number.class.isAssignableFrom(clazz) ||
                clazz.equals(byte.class) ||
                clazz.equals(long.class) ||
                clazz.equals(float.class) ||
                clazz.equals(double.class);
    }

    private static boolean isBoolean(Class<?> clazz)
    {
        return clazz.equals( Boolean.class ) ||
                clazz.equals( boolean.class );
    }

    private static boolean isString(Class<?> clazz)
    {
        return clazz.equals( String.class ) ||
                clazz.equals( char.class );
    }

    private static boolean isJsonArray(Class<?> clazz)
    {
        return clazz.isArray() || Collection.class.isAssignableFrom(clazz);
    }

    private static Class<?> extractTypeFromArray(Type type)
    {
        var parameterType = (ParameterizedType) type;
        return (Class<?>)parameterType.getActualTypeArguments()[0];
    }
}
