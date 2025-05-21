package com.cercademiurentals.api.service;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.exec.http.UpdateExecutionHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cercademiurentals.api.dto.AnuncioCreateDto;
import com.cercademiurentals.api.dto.AnuncioDetalleDto;
import com.cercademiurentals.api.dto.AnuncioSummaryDto;
import com.cercademiurentals.api.dto.AnuncioUpdateDto;
import com.cercademiurentals.api.dto.PuntoDeInteresDto;

import jakarta.annotation.PostConstruct;

@Service
public class SparqlQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SparqlQueryService.class);

    @Value("${FUSEKI_QUERY_ENDPOINT}")
    private String sparqlQueryEndpointUrl;
    @Value("${FUSEKI_UPDATE_ENDPOINT}")
    private String sparqlUpdateEndpointUrl;
    @Value("${FUSEKI_USERNAME}")
    private String fusekiUsername;
    @Value("${FUSEKI_PASSWORD}")
    private String fusekiPassword;

    private static final String BASE_NAMESPACE = "http://www.example.org/cercademiurentals#";
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";
    private static final String RDF_NAMESPACE = RDF.uri;
    private static final String RDFS_NAMESPACE = RDFS.uri;

    private HttpClient authenticatedHttpClient;

    @PostConstruct
    private void initializeHttpClient() {
        logger.info("Iniciando @PostConstruct en SparqlQueryService...");
        logger.info("FUSEKI_QUERY_ENDPOINT: {}", sparqlQueryEndpointUrl);
        logger.info("FUSEKI_UPDATE_ENDPOINT: {}", sparqlUpdateEndpointUrl);
        logger.info("FUSEKI_USERNAME: {}", fusekiUsername != null && !fusekiUsername.isEmpty() ? "Presente" : "AUSENTE o VACÍO");
        logger.info("FUSEKI_PASSWORD: {}", fusekiPassword != null && !fusekiPassword.isEmpty() ? "Presente (longitud: " + fusekiPassword.length() + ")" : "AUSENTE o VACÍA");

        if (sparqlQueryEndpointUrl == null || sparqlUpdateEndpointUrl == null || fusekiUsername == null || fusekiPassword == null ||
            sparqlQueryEndpointUrl.trim().isEmpty() || sparqlUpdateEndpointUrl.trim().isEmpty() || 
            fusekiUsername.trim().isEmpty() || fusekiPassword.trim().isEmpty()) {
            logger.error("¡¡¡ERROR CRÍTICO EN @PostConstruct DE SparqlQueryService!!! Una o más propiedades de conexión a Fuseki no están configuradas. El servicio no funcionará correctamente.");
            return;
        }
        
        logger.info("Inicializando java.net.http.HttpClient para Fuseki con autenticación...");
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fusekiUsername, fusekiPassword.toCharArray());
            }
        };
        this.authenticatedHttpClient = HttpClient.newBuilder()
                .authenticator(authenticator)
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        logger.info("java.net.http.HttpClient para Fuseki inicializado exitosamente en SparqlQueryService.");
    }

    private List<Map<String, String>> executeSelectQuery(String queryString, String methodName) {
        logger.info("Ejecutando SPARQL SELECT desde [{}]...", methodName);
        logger.debug("Query:\n{}", queryString);
        List<Map<String, String>> resultsList = new ArrayList<>();
        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionHTTP.create()
                                        .endpoint(sparqlQueryEndpointUrl)
                                        .query(query)
                                        .httpClient(this.authenticatedHttpClient)
                                        .build()) {
            ResultSet results = qexec.execSelect();
            List<String> resultVars = results.getResultVars();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Map<String, String> resultRow = new HashMap<>();
                for (String var : resultVars) {
                    if (soln.get(var) != null) {
                        resultRow.put(var, soln.get(var).toString());
                    } else {
                        resultRow.put(var, null);
                    }
                }
                resultsList.add(resultRow);
            }
        } catch (Exception e) {
            logger.error("Error [{}] ejecutando la consulta SELECT:\n{}\n", methodName, queryString, e);
             if (e.getMessage() != null && e.getMessage().contains("401")) {
                 logger.error("FALLO DE AUTENTICACIÓN (SELECT): Verifica usuario/contraseña de Fuseki y la configuración.");
             }
            throw new RuntimeException("Error al ejecutar consulta SPARQL SELECT: " + e.getMessage(), e);
        }
        logger.info("[{}] Resultados encontrados: {}", methodName, resultsList.size());
        return resultsList;
    }

    private void executeUpdateQuery(String updateString, String methodName) {
        if ("createAnuncio".equals(methodName) || "updateAnuncio".equals(methodName) || "createSemanticUserStub".equals(methodName) || methodName.startsWith("deleteAnuncio") || methodName.startsWith("markAnuncio") || methodName.startsWith("removeAnuncio") || methodName.startsWith("updateAnuncioState")) {
             logger.info("CONSULTA INSERT/UPDATE/DELETE GENERADA PARA [{}]:\n{}", methodName, updateString);
        } else {
            logger.debug("Ejecutando SPARQL UPDATE desde [{}]:\n{}", methodName, updateString);
        }

        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        try {
             UpdateExecutionHTTP.create()
                .endpoint(sparqlUpdateEndpointUrl)
                .update(updateRequest)
                .httpClient(this.authenticatedHttpClient)
                .execute();
        } catch (Exception e) {
            logger.error("Error [{}] ejecutando SPARQL UPDATE (la consulta está arriba en el log si es relevante, o aquí si es otro método):\n{}\n", methodName, (methodName.startsWith("createAnuncio") || methodName.startsWith("updateAnuncio") || methodName.startsWith("createSemanticUserStub") || methodName.startsWith("deleteAnuncio") || methodName.startsWith("markAnuncio") || methodName.startsWith("removeAnuncio") || methodName.startsWith("updateAnuncioState") ? "VER CONSULTA ARRIBA" : updateString), e);
             if (e.getMessage() != null && e.getMessage().contains("401")) {
                 logger.error("FALLO DE AUTENTICACIÓN (UPDATE): Verifica usuario/contraseña de Fuseki y la configuración.");
             }
            throw new RuntimeException("Error al ejecutar SPARQL UPDATE: " + e.getMessage(), e);
        }
        logger.info("[{}] SPARQL UPDATE ejecutado exitosamente.", methodName);
    }
    
    private String formatXsdString(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return String.format("\"%s\"^^xsd:string", escapeSparqlLiteral(value.trim()));
    }

    private String formatXsdDecimal(Double value) {
        if (value == null) return null;
        return String.format(Locale.US, "\"%.2f\"^^xsd:decimal", value);
    }

    private String formatXsdInteger(Integer value) {
        if (value == null) return null;
        return String.format("\"%d\"^^xsd:integer", value);
    }
    
    private String formatXsdDouble(Double value) {
        if (value == null) return null;
        return String.format(Locale.US, "\"%f\"^^xsd:double", value);
    }

    private String formatXsdBoolean(Boolean value) {
        if (value == null) return null;
        return String.format(Locale.US, "\"%b\"^^xsd:boolean", value);
    }

    private String formatXsdDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            // Intenta parsear primero como LocalDate, luego como DateTime si falla
            try {
                LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                return String.format("\"%s\"^^xsd:date", value.trim());
            } catch (DateTimeParseException e) {
                Instant.parse(value.trim()); // Verifica si es un dateTime válido
                return String.format("\"%s\"^^xsd:date", value.trim().substring(0,10)); // Extrae solo la parte de la fecha
            }
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date/dateTime format for value: '{}'. Will not be included in query.", value, e);
            return null; 
        }
    }
    
    private String formatXsdDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
         try {
            Instant.parse(value.trim()); // Verifica si es un dateTime válido
            return String.format("\"%s\"^^xsd:dateTime", value.trim());
        } catch (DateTimeParseException e) {
            logger.warn("Invalid dateTime format for value: '{}'. Will not be included in query.", value, e);
            return null;
        }
    }

    private String formatXsdAnyURI(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        // Simple validación: debe contener :// y no tener espacios. Podría ser más robusta.
        if (value.contains("://") && !value.contains(" ")) {
            return String.format("<%s>", value.trim());
        }
        logger.warn("Valor inválido para xsd:anyURI: '{}'. No se incluirá.", value);
        return null;
    }
    
    private void addOptionalTriple(List<String> predicateObjects, String predicate, String formattedObject) {
        if (formattedObject != null) {
            predicateObjects.add(String.format("%s %s", predicate, formattedObject));
        }
    }

    private void addOptionalTripleSeparate(StringBuilder queryBuilder, String subjectUri, String predicate, String formattedObject) {
        if (formattedObject != null) {
            queryBuilder.append(String.format("  <%s> %s %s .\n", subjectUri, predicate, formattedObject));
        }
    }

    public void createSemanticUserStub(String userUri, String username, String email, String firstName, String lastName) throws Exception {
        String nowIso = Instant.now().toString(); 

        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append(String.format("PREFIX rdf: <%s>\n", RDF_NAMESPACE));
        insertQuery.append(String.format("PREFIX xsd: <%s>\n", XSD_NAMESPACE));
        insertQuery.append(String.format("PREFIX cerca: <%s>\n", BASE_NAMESPACE));
        insertQuery.append("INSERT DATA {\n");
        insertQuery.append(String.format("  <%s> rdf:type cerca:Usuario ;\n", userUri));
        insertQuery.append(String.format("        cerca:tieneNombreUsuario %s ;\n", formatXsdString(username)));
        insertQuery.append(String.format("        cerca:tieneCorreo %s ;\n", formatXsdString(email)));
        insertQuery.append(String.format("        cerca:tieneNombres %s ;\n", formatXsdString(firstName)));
        insertQuery.append(String.format("        cerca:tieneApellidos %s ;\n", formatXsdString(lastName)));
        insertQuery.append(String.format("        cerca:fechaCreacion %s ;\n", formatXsdDateTime(nowIso)));
        insertQuery.append(String.format("        cerca:ultimoAcceso %s ;\n", formatXsdDateTime(nowIso)));
        insertQuery.append(String.format("        cerca:estaActivo %s ;\n", formatXsdBoolean(true)));
        insertQuery.append(String.format("        cerca:tieneRol <%sRolBuscador> .\n", BASE_NAMESPACE)); 
        insertQuery.append("}\n");

        executeUpdateQuery(insertQuery.toString(), "createSemanticUserStub");
    }
    
    public String createAnuncio(AnuncioCreateDto anuncioData) throws Exception {
        String anuncioId = "anuncio_" + UUID.randomUUID().toString().substring(0, 10).replace("-", "");
        String anuncioUriString = BASE_NAMESPACE + anuncioId;
        String viviendaId = "vivienda_" + UUID.randomUUID().toString().substring(0, 10).replace("-", "");
        String viviendaUriString = BASE_NAMESPACE + viviendaId;
        String nowIso = Instant.now().toString();

        StringBuilder insertDataQuery = new StringBuilder();
        insertDataQuery.append(String.format("PREFIX rdf: <%s>\n", RDF_NAMESPACE));
        insertDataQuery.append(String.format("PREFIX xsd: <%s>\n", XSD_NAMESPACE));
        insertDataQuery.append(String.format("PREFIX cerca: <%s>\n", BASE_NAMESPACE));
        insertDataQuery.append("INSERT DATA {\n");

        List<String> viviendaPredicateObjects = new ArrayList<>();
        viviendaPredicateObjects.add("rdf:type cerca:Vivienda");
        addOptionalTriple(viviendaPredicateObjects, "cerca:tieneDireccion", formatXsdString(anuncioData.getDireccion()));
        addOptionalTriple(viviendaPredicateObjects, "cerca:tieneBarrio", formatXsdString(anuncioData.getBarrio()));
        viviendaPredicateObjects.add("cerca:tieneCiudad \"Florencia\"^^xsd:string"); 
        viviendaPredicateObjects.add("cerca:tieneDepartamento \"Caquetá\"^^xsd:string"); 
        addOptionalTriple(viviendaPredicateObjects, "cerca:tieneTipoVivienda", formatXsdString(anuncioData.getTipoVivienda()));
        addOptionalTriple(viviendaPredicateObjects, "cerca:tieneLatitud", formatXsdDouble(anuncioData.getLatitud()));
        addOptionalTriple(viviendaPredicateObjects, "cerca:tieneLongitud", formatXsdDouble(anuncioData.getLongitud()));
        addOptionalTriple(viviendaPredicateObjects, "cerca:numeroTotalHabitaciones", formatXsdInteger(anuncioData.getNumeroTotalHabitaciones()));
        addOptionalTriple(viviendaPredicateObjects, "cerca:numeroTotalBanos", formatXsdInteger(anuncioData.getNumeroTotalBanos()));
        
        if (viviendaPredicateObjects.size() > 1) {
            insertDataQuery.append(String.format("  <%s> %s .\n", viviendaUriString, String.join(" ;\n        ", viviendaPredicateObjects)));
        } else if (!viviendaPredicateObjects.isEmpty()){ 
             insertDataQuery.append(String.format("  <%s> %s .\n", viviendaUriString, viviendaPredicateObjects.get(0)));
        }
        addOptionalTripleSeparate(insertDataQuery, viviendaUriString, "cerca:tieneMetrosCuadrados", formatXsdInteger(anuncioData.getMetrosCuadrados()));

        List<String> anuncioPredicateObjects = new ArrayList<>();
        anuncioPredicateObjects.add("rdf:type cerca:Anuncio");
        if (anuncioData.getProviderUri() != null && !anuncioData.getProviderUri().trim().isEmpty()) {
             anuncioPredicateObjects.add(String.format("cerca:esPublicadoPor <%s>", anuncioData.getProviderUri().trim()));
        } else {
            logger.error("CRITICAL: Provider URI es null o vacío para el nuevo anuncio.");
            throw new IllegalArgumentException("Provider URI no puede ser nulo o vacío para crear un anuncio.");
        }
        anuncioPredicateObjects.add(String.format("cerca:describeVivienda <%s>", viviendaUriString));
        addOptionalTriple(anuncioPredicateObjects, "cerca:tieneTitulo", formatXsdString(anuncioData.getTitulo()));
        addOptionalTriple(anuncioPredicateObjects, "cerca:tieneTerminoContrato", formatXsdString(anuncioData.getTerminoContrato()));
        anuncioPredicateObjects.add("cerca:tieneEstadoAnuncio \"Disponible\"^^xsd:string"); 
        anuncioPredicateObjects.add(String.format("cerca:fechaCreacionAnuncio %s", formatXsdDateTime(nowIso)));
        addOptionalTriple(anuncioPredicateObjects, "cerca:tienePrecioMonto", formatXsdDecimal(anuncioData.getPrecioMonto()));
        
        String esCompartidoFormatted = formatXsdBoolean(anuncioData.getEsAnuncioCompartido());
        if (esCompartidoFormatted != null) { 
            addOptionalTriple(anuncioPredicateObjects, "cerca:esAnuncioCompartido", esCompartidoFormatted);
        }
        addOptionalTriple(anuncioPredicateObjects, "cerca:numeroHabitacionesDisponibles", formatXsdInteger(anuncioData.getNumeroHabitacionesDisponibles()));
        addOptionalTriple(anuncioPredicateObjects, "cerca:fechaDisponibleDesde", formatXsdDate(anuncioData.getFechaDisponibleDesde()));
        
        if (anuncioPredicateObjects.size() > 1) {
             insertDataQuery.append(String.format("  <%s> %s .\n", anuncioUriString, String.join(" ;\n        ", anuncioPredicateObjects)));
        } else if (!anuncioPredicateObjects.isEmpty()){
             insertDataQuery.append(String.format("  <%s> %s .\n", anuncioUriString, anuncioPredicateObjects.get(0)));
        }

        addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:tieneDescripcionDetallada", formatXsdString(anuncioData.getDescripcionDetallada()));
        addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:numeroBanosDisponibles", formatXsdInteger(anuncioData.getNumeroBanosDisponibles()));
        String permiteMascotasFormatted = formatXsdBoolean(anuncioData.getPermiteMascotas());
        if (permiteMascotasFormatted != null) addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:permiteMascotas", permiteMascotasFormatted);
        String permiteFumarFormatted = formatXsdBoolean(anuncioData.getPermiteFumar());
        if (permiteFumarFormatted != null) addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:permiteFumar", permiteFumarFormatted);
        String permiteInvitadosFormatted = formatXsdBoolean(anuncioData.getPermiteInvitados());
        if (permiteInvitadosFormatted != null) addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:permiteInvitados", permiteInvitadosFormatted);
        String soloEstudiantesFormatted = formatXsdBoolean(anuncioData.getSoloEstudiantes());
        if (soloEstudiantesFormatted != null) addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:soloEstudiantes", soloEstudiantesFormatted);
        
        String preferenciaGenero = anuncioData.getPreferenciaGenero();
        if (preferenciaGenero != null && !preferenciaGenero.trim().isEmpty() && !"Indiferente".equalsIgnoreCase(preferenciaGenero.trim())) {
            addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:preferenciaGenero", formatXsdString(preferenciaGenero));
        }

        String estableceHorasSilencioFormatted = formatXsdBoolean(anuncioData.getEstableceHorasSilencio());
        if (estableceHorasSilencioFormatted != null) addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:estableceHorasSilencio", estableceHorasSilencioFormatted);

        if (anuncioData.getImagenUrl() != null && !anuncioData.getImagenUrl().trim().isEmpty()) {
             addOptionalTripleSeparate(insertDataQuery, anuncioUriString, "cerca:tieneUrlFoto", formatXsdAnyURI(anuncioData.getImagenUrl()));
        }


        if (anuncioData.getComodidadesUris() != null && !anuncioData.getComodidadesUris().isEmpty()) {
            for (String comUri : anuncioData.getComodidadesUris()) {
                if (comUri != null && !comUri.trim().isEmpty() && comUri.startsWith(BASE_NAMESPACE)) { 
                    insertDataQuery.append(String.format("  <%s> cerca:incluyeComodidad <%s> .\n", anuncioUriString, comUri.trim()));
                } else {
                    logger.warn("URI de comodidad inválida o no perteneciente al namespace base, omitiendo: {}", comUri);
                }
            }
        }
        insertDataQuery.append("}\n");
        String finalInsertQuery = insertDataQuery.toString();
        executeUpdateQuery(finalInsertQuery, "createAnuncio");
        return anuncioUriString;
    }
    
    public int countUsers() { 
        String countQuery = String.format("""
            PREFIX rdf: <%s> PREFIX cerca: <%s>
            SELECT (COUNT(?user) AS ?count) WHERE { ?user rdf:type cerca:Usuario . }
            """, RDF_NAMESPACE, BASE_NAMESPACE);
        try {
             List<Map<String, String>> results = executeSelectQuery(countQuery, "countUsers");
             if (!results.isEmpty() && results.get(0).containsKey("count")) {
                 return extractIntegerValue(results.get(0).get("count"));
             }
        } catch (Exception e) { /* Error ya logueado */ }
        return 0;
    }
    
    public List<AnuncioSummaryDto> findAvailableAnuncios(
            String searchText, String tipoVivienda, Double precioMax,
            Integer numeroHabitacionesMin, List<String> comodidadesUris,
            String pdiUriFilter, Double distanciaMaxKmFilter
            ) throws Exception {
        
        StringBuilder queryPatterns = new StringBuilder();
        StringBuilder queryFilters = new StringBuilder();
        
        queryPatterns.append("""
              ?anuncioUri rdf:type cerca:Anuncio ;
                          cerca:tieneEstadoAnuncio "Disponible"^^xsd:string ;
                          cerca:tieneTitulo ?titulo ;
                          cerca:tienePrecioMonto ?precio .
              ?anuncioUri cerca:describeVivienda ?vivienda .
              ?vivienda rdf:type cerca:Vivienda ;
                        cerca:tieneLatitud ?lat ;
                        cerca:tieneLongitud ?lon .
              OPTIONAL { ?vivienda cerca:tieneBarrio ?barrio . }
              OPTIONAL { ?vivienda cerca:tieneTipoVivienda ?tipoActualVivienda .}
              OPTIONAL { ?anuncioUri cerca:numeroHabitacionesDisponibles ?numHab .}
              OPTIONAL { ?anuncioUri cerca:fechaCreacionAnuncio ?fechaCreacionAnuncio . } 
              OPTIONAL { ?anuncioUri cerca:tieneUrlFoto ?imagenUrl . }
            """);

        if (searchText != null && !searchText.trim().isEmpty()) {
            queryPatterns.append("  OPTIONAL { ?anuncioUri cerca:tieneDescripcionDetallada ?descripcionDetallada . } \n");
            queryPatterns.append("  OPTIONAL { ?vivienda cerca:tieneBarrio ?barrioSearch . } \n"); 
            String escSearch = escapeSparqlLiteral(searchText.trim().toLowerCase());
            queryFilters.append(String.format(
                "  FILTER (CONTAINS(LCASE(?titulo), \"%s\") || (BOUND(?descripcionDetallada) && CONTAINS(LCASE(?descripcionDetallada), \"%s\")) || (BOUND(?barrioSearch) && CONTAINS(LCASE(?barrioSearch), \"%s\"))) .\n", 
                escSearch, escSearch, escSearch));
        }
        if (tipoVivienda != null && !tipoVivienda.trim().isEmpty()) {
            queryFilters.append(String.format("  FILTER (STR(?tipoActualVivienda) = %s) .\n", formatXsdString(tipoVivienda)));
        }
        if (numeroHabitacionesMin != null && numeroHabitacionesMin > 0) {
            queryFilters.append(String.format("  FILTER (?numHab >= %d) .\n", numeroHabitacionesMin));
        }
        if (precioMax != null && precioMax >= 0) {
             queryFilters.append(String.format(Locale.US, "  FILTER (xsd:decimal(?precio) <= %.2f) .\n", precioMax));
        }
        if (comodidadesUris != null && !comodidadesUris.isEmpty()) {
            for (String comUri : comodidadesUris) {
                if (comUri != null && comUri.startsWith(BASE_NAMESPACE)) {
                    queryFilters.append(String.format("  FILTER EXISTS { ?anuncioUri cerca:incluyeComodidad <%s> } .\n", comUri));
                }
            }
        }
        
        String finalQueryString = String.format("""
            PREFIX rdf: <%s> PREFIX xsd: <%s> PREFIX cerca: <%s>
            SELECT DISTINCT ?anuncioUri ?titulo ?precio ?lat ?lon ?barrio ?tipoActualVivienda ?fechaCreacionAnuncio ?imagenUrl
            WHERE { 
                %s 
                %s 
            }
            ORDER BY ?titulo
            """, RDF_NAMESPACE, XSD_NAMESPACE, BASE_NAMESPACE, queryPatterns.toString(), queryFilters.toString());

        List<AnuncioSummaryDto> anuncios = new ArrayList<>();
        List<Map<String, String>> results = executeSelectQuery(finalQueryString, "findAvailableAnuncios_Initial");

        for (Map<String, String> sol : results) {
            AnuncioSummaryDto dto = new AnuncioSummaryDto();
            dto.setUri(extractUriValue(sol.get("anuncioUri")));
            dto.setTitulo(extractStringValue(sol.get("titulo")));
            dto.setPrecio(extractDoubleValue(sol.get("precio")));
            dto.setLatitud(extractDoubleValue(sol.get("lat")));
            dto.setLongitud(extractDoubleValue(sol.get("lon")));
            dto.setBarrio(extractStringValue(sol.get("barrio"))); 
            dto.setTipoVivienda(extractStringValue(sol.get("tipoActualVivienda")));
            dto.setFechaCreacionAnuncio(extractStringValue(sol.get("fechaCreacionAnuncio"))); 
            dto.setImagenUrl(extractStringValue(sol.get("imagenUrl")));
            anuncios.add(dto);
        }

        if (pdiUriFilter != null && !pdiUriFilter.trim().isEmpty() && distanciaMaxKmFilter != null && distanciaMaxKmFilter > 0) {
            logger.info("Aplicando filtro de proximidad: PDI URI={}, Distancia Max={}km", pdiUriFilter, distanciaMaxKmFilter);
            Optional<PuntoDeInteresDto> pdiSeleccionadoOpt = findPuntoDeInteresByUri(pdiUriFilter); 

            if (pdiSeleccionadoOpt.isPresent()) {
                PuntoDeInteresDto pdiSeleccionado = pdiSeleccionadoOpt.get();
                if (pdiSeleccionado.getLatitud() != null && pdiSeleccionado.getLongitud() != null) {
                    double pdiLat = pdiSeleccionado.getLatitud();
                    double pdiLon = pdiSeleccionado.getLongitud();
                    anuncios = anuncios.stream()
                        .filter(anuncio -> {
                            if (anuncio.getLatitud() != null && anuncio.getLongitud() != null) {
                                double distancia = calcularDistanciaHaversine(
                                    pdiLat, pdiLon,
                                    anuncio.getLatitud(), anuncio.getLongitud()
                                );
                                return distancia <= distanciaMaxKmFilter;
                            }
                            return false; 
                        })
                        .collect(Collectors.toList());
                    logger.info("Anuncios después del filtro de proximidad: {}", anuncios.size());
                } else {
                    logger.warn("PDI seleccionado ({}) para filtro de proximidad no tiene coordenadas válidas.", pdiUriFilter);
                }
            } else {
                logger.warn("PDI seleccionado ({}) para filtro de proximidad no encontrado.", pdiUriFilter);
            }
        }
        return anuncios;
    }

    public Optional<PuntoDeInteresDto> findPuntoDeInteresByUri(String pdiUri) throws Exception {
        String queryString = String.format("""
            PREFIX rdfs: <%s>
            PREFIX cerca: <%s>
            SELECT ?nombre ?latitud ?longitud WHERE {
                BIND(IRI("%s") AS ?pdi)
                ?pdi rdfs:label ?labelNombre .
                BIND(STR(?labelNombre) AS ?nombre)
                ?pdi cerca:tieneLatitud ?latitud .
                ?pdi cerca:tieneLongitud ?longitud .
            } LIMIT 1
            """, RDFS_NAMESPACE, BASE_NAMESPACE, pdiUri);

        List<Map<String, String>> results = executeSelectQuery(queryString, "findPuntoDeInteresByUri");
        if (!results.isEmpty()) {
            Map<String, String> sol = results.get(0);
            PuntoDeInteresDto dto = new PuntoDeInteresDto();
            dto.setUri(pdiUri);
            dto.setNombre(extractStringValue(sol.get("nombre")));
            dto.setLatitud(extractDoubleValue(sol.get("latitud")));
            dto.setLongitud(extractDoubleValue(sol.get("longitud")));
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    private static final double RADIO_TIERRA_KM = 6371.0;

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }

    public List<PuntoDeInteresDto> findPuntosDeInteres() throws Exception {
        String queryString = String.format("""
            PREFIX rdf: <%s>
            PREFIX rdfs: <%s>
            PREFIX cerca: <%s>
            SELECT ?pdiUri ?nombre ?latitud ?longitud
            WHERE {
              { ?pdiUri rdf:type cerca:Universidad . }
              UNION
              { ?pdiUri rdf:type cerca:LugarReferencia . }
              UNION
              { ?pdiUri rdf:type cerca:Empresa . }
              ?pdiUri rdfs:label ?labelNombre .
              BIND(STR(?labelNombre) As ?nombre) 
              ?pdiUri cerca:tieneLatitud ?latitud .
              ?pdiUri cerca:tieneLongitud ?longitud .
            }
            ORDER BY ?nombre
            """, RDF_NAMESPACE, RDFS_NAMESPACE, BASE_NAMESPACE);

        List<PuntoDeInteresDto> pdiList = new ArrayList<>();
        List<Map<String, String>> results = executeSelectQuery(queryString, "findPuntosDeInteres");
        for (Map<String, String> sol : results) {
            PuntoDeInteresDto dto = new PuntoDeInteresDto();
            dto.setUri(extractUriValue(sol.get("pdiUri")));
            dto.setNombre(extractStringValue(sol.get("nombre"))); 
            dto.setLatitud(extractDoubleValue(sol.get("latitud")));
            dto.setLongitud(extractDoubleValue(sol.get("longitud")));
            pdiList.add(dto);
        }
        logger.info("Se encontraron {} Puntos de Interés.", pdiList.size());
        return pdiList;
    }
    
    public Optional<AnuncioDetalleDto> findAnuncioDetailsByUri(String anuncioUri) throws Exception {
         String queryString = String.format("""
            PREFIX rdf: <%s> PREFIX xsd: <%s> PREFIX cerca: <%s> PREFIX rdfs: <%s>
            SELECT * WHERE { 
              BIND(IRI("%s") AS ?anuncioUri)
              ?anuncioUri rdf:type cerca:Anuncio ; 
                          cerca:tieneTitulo ?titulo ; 
                          cerca:tienePrecioMonto ?precioMonto ;
                          cerca:tieneTerminoContrato ?terminoContrato ; 
                          cerca:esAnuncioCompartido ?esAnuncioCompartido ;
                          cerca:numeroHabitacionesDisponibles ?numeroHabitacionesDisponibles ; 
                          cerca:fechaDisponibleDesde ?fechaDisponibleDesde ;
                          cerca:tieneEstadoAnuncio ?estadoAnuncio ; 
                          cerca:fechaCreacionAnuncio ?fechaCreacionAnuncio .
              OPTIONAL { ?anuncioUri cerca:tieneDescripcionDetallada ?descripcionDetallada . }
              OPTIONAL { ?anuncioUri cerca:numeroBanosDisponibles ?numeroBanosDisponibles . }
              OPTIONAL { ?anuncioUri cerca:fechaUltimaActualizacion ?fechaUltimaActualizacion . }
              OPTIONAL { ?anuncioUri cerca:permiteMascotas ?permiteMascotas . } 
              OPTIONAL { ?anuncioUri cerca:permiteFumar ?permiteFumar . }
              OPTIONAL { ?anuncioUri cerca:permiteInvitados ?permiteInvitados . } 
              OPTIONAL { ?anuncioUri cerca:soloEstudiantes ?soloEstudiantes . }
              OPTIONAL { ?anuncioUri cerca:preferenciaGenero ?preferenciaGenero . } 
              OPTIONAL { ?anuncioUri cerca:estableceHorasSilencio ?estableceHorasSilencio . }
              OPTIONAL { ?anuncioUri cerca:tieneUrlFoto ?imagenUrl . }
              
              ?anuncioUri cerca:describeVivienda ?viviendaUri .
              ?viviendaUri rdf:type cerca:Vivienda ; 
                           cerca:tieneDireccion ?direccion ; 
                           cerca:tieneBarrio ?barrio ; 
                           cerca:tieneCiudad ?ciudad ;
                           cerca:tieneDepartamento ?departamento ; 
                           cerca:tieneLatitud ?latitud ; 
                           cerca:tieneLongitud ?longitud ;
                           cerca:tieneTipoVivienda ?tipoVivienda ; 
                           cerca:numeroTotalHabitaciones ?numeroTotalHabitaciones ; 
                           cerca:numeroTotalBanos ?numeroTotalBanos .
              OPTIONAL { ?viviendaUri cerca:tieneMetrosCuadrados ?metrosCuadrados . }
              
              ?anuncioUri cerca:esPublicadoPor ?proveedorUri .
              ?proveedorUri rdf:type cerca:Usuario ; 
                            cerca:tieneNombres ?provNombres ; 
                            cerca:tieneApellidos ?provApellidos ; 
                            cerca:tieneNombreUsuario ?provUsername .
              OPTIONAL { ?proveedorUri cerca:tieneCalificacionPromedio ?provCalificacion . }
              
              { 
                SELECT ?anuncioUri (GROUP_CONCAT(DISTINCT STR(?labelComodidad); separator="||") AS ?comodidadesConcat)
                WHERE { 
                  BIND(IRI("%s") AS ?anuncioUri) 
                  OPTIONAL { 
                    ?anuncioUri cerca:incluyeComodidad ?comodidadUri . 
                    ?comodidadUri rdfs:label ?labelComodidad . 
                  } 
                } GROUP BY ?anuncioUri 
              }
            } LIMIT 1 
            """, RDF_NAMESPACE, XSD_NAMESPACE, BASE_NAMESPACE, RDFS_NAMESPACE, anuncioUri, anuncioUri);

        List<Map<String, String>> results = executeSelectQuery(queryString, "findAnuncioDetailsByUri");
        if (!results.isEmpty()) {
            Map<String, String> sol = results.get(0);
            AnuncioDetalleDto dto = new AnuncioDetalleDto();
            AnuncioDetalleDto.ProveedorDto provDto = new AnuncioDetalleDto.ProveedorDto();
            
            dto.setUri(anuncioUri); 
            dto.setTitulo(extractStringValue(sol.get("titulo")));
            dto.setDescripcionDetallada(extractStringValue(sol.get("descripcionDetallada")));
            dto.setPrecioMonto(extractDoubleValue(sol.get("precioMonto")));
            dto.setTerminoContrato(extractStringValue(sol.get("terminoContrato")));
            dto.setEsAnuncioCompartido(extractBooleanValue(sol.get("esAnuncioCompartido")));
            dto.setNumeroHabitacionesDisponibles(extractIntegerValue(sol.get("numeroHabitacionesDisponibles")));
            dto.setNumeroBanosDisponibles(extractIntegerValue(sol.get("numeroBanosDisponibles")));
            dto.setFechaDisponibleDesde(extractStringValue(sol.get("fechaDisponibleDesde")));
            dto.setFechaCreacionAnuncio(extractStringValue(sol.get("fechaCreacionAnuncio")));
            dto.setFechaUltimaActualizacion(extractStringValue(sol.get("fechaUltimaActualizacion")));
            dto.setEstadoAnuncio(extractStringValue(sol.get("estadoAnuncio")));
            dto.setPermiteMascotas(extractBooleanValue(sol.get("permiteMascotas")));
            dto.setPermiteFumar(extractBooleanValue(sol.get("permiteFumar")));
            dto.setPermiteInvitados(extractBooleanValue(sol.get("permiteInvitados")));
            dto.setSoloEstudiantes(extractBooleanValue(sol.get("soloEstudiantes")));
            dto.setPreferenciaGenero(extractStringValue(sol.get("preferenciaGenero")));
            dto.setEstableceHorasSilencio(extractBooleanValue(sol.get("estableceHorasSilencio")));
            dto.setImagenUrl(extractStringValue(sol.get("imagenUrl")));
            
            dto.setViviendaUri(extractUriValue(sol.get("viviendaUri")));
            dto.setDireccion(extractStringValue(sol.get("direccion")));
            dto.setBarrio(extractStringValue(sol.get("barrio")));
            dto.setCiudad(extractStringValue(sol.get("ciudad")));
            dto.setDepartamento(extractStringValue(sol.get("departamento")));
            dto.setLatitud(extractDoubleValue(sol.get("latitud")));
            dto.setLongitud(extractDoubleValue(sol.get("longitud")));
            dto.setTipoVivienda(extractStringValue(sol.get("tipoVivienda")));
            dto.setNumeroTotalHabitaciones(extractIntegerValue(sol.get("numeroTotalHabitaciones")));
            dto.setNumeroTotalBanos(extractIntegerValue(sol.get("numeroTotalBanos")));
            dto.setMetrosCuadrados(extractIntegerValue(sol.get("metrosCuadrados")));
            
            provDto.setUri(extractUriValue(sol.get("proveedorUri")));
            provDto.setNombres(extractStringValue(sol.get("provNombres")));
            provDto.setApellidos(extractStringValue(sol.get("provApellidos")));
            provDto.setNombreUsuario(extractStringValue(sol.get("provUsername")));
            provDto.setCalificacionPromedio(extractFloatValue(sol.get("provCalificacion")));
            dto.setProveedor(provDto);
            
            String comStr = extractStringValue(sol.get("comodidadesConcat"));
            dto.setComodidades( (comStr == null || comStr.isEmpty()) ? new ArrayList<>() : List.of(comStr.split("\\|\\|")));
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    public List<AnuncioSummaryDto> findAnunciosByProviderUri(String providerUri) throws Exception {
         String queryString = String.format("""
            PREFIX rdf: <%s> PREFIX xsd: <%s> PREFIX cerca: <%s>
            SELECT ?anuncioUri ?titulo ?precio ?lat ?lon ?estado ?barrio ?tipoVivienda ?fechaCreacionAnuncio ?imagenUrl WHERE {
              BIND(IRI("%s") AS ?provider)
              ?anuncioUri rdf:type cerca:Anuncio ; 
                          cerca:esPublicadoPor ?provider ; 
                          cerca:tieneTitulo ?titulo ;
                          cerca:tienePrecioMonto ?precio ; 
                          cerca:tieneEstadoAnuncio ?estado ;
                          cerca:fechaCreacionAnuncio ?fechaCreacionAnuncio . 
              OPTIONAL { ?anuncioUri cerca:tieneUrlFoto ?imagenUrl . }
              ?anuncioUri cerca:describeVivienda ?vivienda .
              ?vivienda rdf:type cerca:Vivienda ; 
                        cerca:tieneLatitud ?lat ; 
                        cerca:tieneLongitud ?lon .
              OPTIONAL { ?vivienda cerca:tieneBarrio ?barrio . }
              OPTIONAL { ?vivienda cerca:tieneTipoVivienda ?tipoVivienda . }
            } ORDER BY DESC(?fechaCreacionAnuncio)""", RDF_NAMESPACE, XSD_NAMESPACE, BASE_NAMESPACE, providerUri); 

        List<AnuncioSummaryDto> anuncios = new ArrayList<>();
         try {
            List<Map<String, String>> results = executeSelectQuery(queryString, "findAnunciosByProviderUri");
            for (Map<String, String> sol : results) {
                AnuncioSummaryDto dto = new AnuncioSummaryDto();
                dto.setUri(extractUriValue(sol.get("anuncioUri")));
                dto.setTitulo(extractStringValue(sol.get("titulo")));
                dto.setPrecio(extractDoubleValue(sol.get("precio")));
                dto.setLatitud(extractDoubleValue(sol.get("lat")));
                dto.setLongitud(extractDoubleValue(sol.get("lon")));
                dto.setEstadoAnuncio(extractStringValue(sol.get("estado"))); 
                dto.setBarrio(extractStringValue(sol.get("barrio")));
                dto.setTipoVivienda(extractStringValue(sol.get("tipoVivienda")));
                dto.setFechaCreacionAnuncio(extractStringValue(sol.get("fechaCreacionAnuncio")));
                dto.setImagenUrl(extractStringValue(sol.get("imagenUrl")));
                anuncios.add(dto);
            }
        } catch (Exception e) {
            throw e;
        }
        return anuncios;
    }

    public void deleteAnuncioByUri(String anuncioUri) throws Exception {
        String selectViviendaQuery = String.format("""
            PREFIX cerca: <%s>
            SELECT ?viviendaUri WHERE {
                <%s> cerca:describeVivienda ?viviendaUri .
            }
            """, BASE_NAMESPACE, anuncioUri);
        
        String viviendaUriToDelete = null;
        List<Map<String, String>> viviendaResult = executeSelectQuery(selectViviendaQuery, "findViviendaForDelete");
        if (!viviendaResult.isEmpty() && viviendaResult.get(0).get("viviendaUri") != null) {
            viviendaUriToDelete = extractUriValue(viviendaResult.get(0).get("viviendaUri"));
        }

        String deleteAnuncioSujeto = String.format("DELETE WHERE { <%s> ?p ?o . }", anuncioUri);
        String deleteAnuncioObjeto = String.format("DELETE WHERE { ?s ?p <%s> . }", anuncioUri);
        
        try {
            logger.info("Ejecutando DELETE (sujeto) para el anuncio: {}", anuncioUri);
            executeUpdateQuery(deleteAnuncioSujeto, "deleteAnuncioByUri_AnuncioSujeto");
            
            logger.info("Ejecutando DELETE (objeto) para referencias al anuncio: {}", anuncioUri);
            executeUpdateQuery(deleteAnuncioObjeto, "deleteAnuncioByUri_AnuncioObjeto");

            if (viviendaUriToDelete != null) {
                String deleteViviendaSujeto = String.format("DELETE WHERE { <%s> ?pv ?ov . }", viviendaUriToDelete);
                logger.info("Ejecutando DELETE (sujeto) para la vivienda asociada: {}", viviendaUriToDelete);
                executeUpdateQuery(deleteViviendaSujeto, "deleteAnuncioByUri_ViviendaSujeto");
                String deleteViviendaObjeto = String.format("DELETE WHERE { ?sv ?pv <%s> . }", viviendaUriToDelete);
                logger.info("Ejecutando DELETE (objeto) para referencias a la vivienda: {}", viviendaUriToDelete);
                executeUpdateQuery(deleteViviendaObjeto, "deleteAnuncioByUri_ViviendaObjeto");
            }
            
            logger.info("Anuncio y su vivienda asociada (si aplica) eliminados: {}", anuncioUri);
        } catch (Exception e) {
            logger.error("Excepción al intentar eliminar el anuncio {}: {}", anuncioUri, e.getMessage(), e);
            throw e; 
        }
    }

    public boolean updateAnuncio(String anuncioUri, AnuncioUpdateDto anuncioData) throws Exception {
        logger.info("Iniciando actualización para el anuncio: {}", anuncioUri);

        String viviendaUri = null;
        String selectViviendaQuery = String.format("""
            PREFIX cerca: <%s>
            SELECT ?viviendaUri WHERE {
                <%s> cerca:describeVivienda ?viviendaUri .
            }
            """, BASE_NAMESPACE, anuncioUri);
        
        List<Map<String, String>> viviendaResult = executeSelectQuery(selectViviendaQuery, "findViviendaForUpdate");
        if (!viviendaResult.isEmpty() && viviendaResult.get(0).get("viviendaUri") != null) {
            viviendaUri = extractUriValue(viviendaResult.get(0).get("viviendaUri"));
            logger.debug("Vivienda asociada encontrada: {}", viviendaUri);
        } else {
            logger.error("No se pudo encontrar la vivienda asociada al anuncio {} para la actualización.", anuncioUri);
            throw new IllegalArgumentException("Vivienda asociada no encontrada para el anuncio: " + anuncioUri + ". No se puede actualizar.");
        }

        String nowIso = Instant.now().toString(); 

        StringBuilder deleteClause = new StringBuilder();
        StringBuilder insertClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder(); 

        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:fechaUltimaActualizacion", "?oldFechaActualizacion", formatXsdDateTime(nowIso));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:tieneTitulo", "?oldTitulo", formatXsdString(anuncioData.getTitulo()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:tieneDescripcionDetallada", "?oldDesc", formatXsdString(anuncioData.getDescripcionDetallada()), true); 
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:tienePrecioMonto", "?oldPrecio", formatXsdDecimal(anuncioData.getPrecioMonto()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:tieneTerminoContrato", "?oldTermino", formatXsdString(anuncioData.getTerminoContrato()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:esAnuncioCompartido", "?oldCompartido", formatXsdBoolean(anuncioData.getEsAnuncioCompartido()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:numeroHabitacionesDisponibles", "?oldNumHab", formatXsdInteger(anuncioData.getNumeroHabitacionesDisponibles()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:numeroBanosDisponibles", "?oldNumBanosAnuncio", formatXsdInteger(anuncioData.getNumeroBanosDisponibles()), true); 
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:fechaDisponibleDesde", "?oldFechaDisp", formatXsdDate(anuncioData.getFechaDisponibleDesde()));
        
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:permiteMascotas", "?oldMascotas", formatXsdBoolean(anuncioData.getPermiteMascotas()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:permiteFumar", "?oldFumar", formatXsdBoolean(anuncioData.getPermiteFumar()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:permiteInvitados", "?oldInvitados", formatXsdBoolean(anuncioData.getPermiteInvitados()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:soloEstudiantes", "?oldEstudiantes", formatXsdBoolean(anuncioData.getSoloEstudiantes()));
        
        String preferenciaGenero = anuncioData.getPreferenciaGenero();
        if (preferenciaGenero != null && !preferenciaGenero.trim().isEmpty() && !"Indiferente".equalsIgnoreCase(preferenciaGenero.trim())) {
            addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:preferenciaGenero", "?oldPrefGenero", formatXsdString(preferenciaGenero));
        } else { 
            deleteClause.append(String.format("  <%s> cerca:preferenciaGenero ?oldPrefGeneroVal .\n", anuncioUri));
            whereClause.append(String.format("  OPTIONAL { <%s> cerca:preferenciaGenero ?oldPrefGeneroVal . }\n", anuncioUri));
        }
        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:estableceHorasSilencio", "?oldSilencio", formatXsdBoolean(anuncioData.getEstableceHorasSilencio()));

        addPropertyToUpdate(deleteClause, insertClause, whereClause, anuncioUri, "cerca:tieneUrlFoto", "?oldImagenUrl", 
            (anuncioData.getImagenUrl() != null && !anuncioData.getImagenUrl().trim().isEmpty()) ? formatXsdAnyURI(anuncioData.getImagenUrl()) : null, true);


        deleteClause.append(String.format("  <%s> cerca:incluyeComodidad ?anyOldComodidad .\n", anuncioUri));
        whereClause.append(String.format("  OPTIONAL { <%s> cerca:incluyeComodidad ?anyOldComodidad . }\n", anuncioUri)); 
        if (anuncioData.getComodidadesUris() != null && !anuncioData.getComodidadesUris().isEmpty()) {
            for (String comUri : anuncioData.getComodidadesUris()) {
                if (comUri != null && !comUri.trim().isEmpty() && comUri.startsWith(BASE_NAMESPACE)) {
                    insertClause.append(String.format("  <%s> cerca:incluyeComodidad <%s> .\n", anuncioUri, comUri.trim()));
                }
            }
        }
        
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneDireccion", "?oldDir", formatXsdString(anuncioData.getDireccion()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneBarrio", "?oldBarrio", formatXsdString(anuncioData.getBarrio()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneTipoVivienda", "?oldTipoViv", formatXsdString(anuncioData.getTipoVivienda()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneLatitud", "?oldLat", formatXsdDouble(anuncioData.getLatitud()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneLongitud", "?oldLon", formatXsdDouble(anuncioData.getLongitud()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:numeroTotalHabitaciones", "?oldTotalHabViv", formatXsdInteger(anuncioData.getNumeroTotalHabitaciones()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:numeroTotalBanos", "?oldTotalBanosViv", formatXsdInteger(anuncioData.getNumeroTotalBanos()));
        addPropertyToUpdate(deleteClause, insertClause, whereClause, viviendaUri, "cerca:tieneMetrosCuadrados", "?oldMetros", formatXsdInteger(anuncioData.getMetrosCuadrados()), true); 

        if (deleteClause.length() == 0 && insertClause.length() == 0) {
            logger.info("No hay cambios detectados para el anuncio {} y su vivienda asociada. Solo se actualizará fechaUltimaActualizacion.", anuncioUri);
            String updateFechaQuery = String.format("""
                PREFIX cerca: <%s> PREFIX xsd: <%s>
                DELETE { <%s> cerca:fechaUltimaActualizacion ?oldFUA . }
                INSERT { <%s> cerca:fechaUltimaActualizacion %s . }
                WHERE { OPTIONAL { <%s> cerca:fechaUltimaActualizacion ?oldFUA . } }
                """, BASE_NAMESPACE, XSD_NAMESPACE, anuncioUri, anuncioUri, formatXsdDateTime(nowIso), anuncioUri);
            executeUpdateQuery(updateFechaQuery, "updateAnuncio_FechaForzada");
            return true; 
        }

        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append(String.format("PREFIX cerca: <%s>\n", BASE_NAMESPACE));
        updateQuery.append(String.format("PREFIX xsd: <%s>\n", XSD_NAMESPACE));
        
        if (deleteClause.length() > 0) {
            updateQuery.append("DELETE {\n");
            updateQuery.append(deleteClause.toString());
            updateQuery.append("}\n");
        }
        if (insertClause.length() > 0) {
            updateQuery.append("INSERT {\n");
            updateQuery.append(insertClause.toString());
            updateQuery.append("}\n");
        }
        
        updateQuery.append("WHERE {\n");
        updateQuery.append(String.format("  BIND(IRI(\"%s\") AS ?anuncioUri) \n", anuncioUri));
        updateQuery.append(String.format("  BIND(IRI(\"%s\") AS ?viviendaUri) \n", viviendaUri)); 
        updateQuery.append("  ?anuncioUri cerca:describeVivienda ?viviendaUri .\n"); 
        
        if (whereClause.length() > 0) {
             updateQuery.append(whereClause.toString());
        }
        updateQuery.append("}\n");

        String finalUpdateQuery = updateQuery.toString();
        executeUpdateQuery(finalUpdateQuery, "updateAnuncio");
        return true;
    }

    private void addPropertyToUpdate(StringBuilder deleteClause, StringBuilder insertClause, StringBuilder whereClause,
                                     String subjectUri, String predicate, String oldVar, String newFormattedValue, boolean allowNullOrEmptyStringMeansDelete) {
        deleteClause.append(String.format("  <%s> %s %s .\n", subjectUri, predicate, oldVar));
        whereClause.append(String.format("  OPTIONAL { <%s> %s %s . }\n", subjectUri, predicate, oldVar));

        if (newFormattedValue != null) { 
            insertClause.append(String.format("  <%s> %s %s .\n", subjectUri, predicate, newFormattedValue));
        } else if (allowNullOrEmptyStringMeansDelete) { 
            // No se añade al INSERT, efectivamente eliminando la tripleta si existía.
        }
    }
    private void addPropertyToUpdate(StringBuilder deleteClause, StringBuilder insertClause, StringBuilder whereClause,
                                     String subjectUri, String predicate, String oldVar, String newFormattedValue) {
        addPropertyToUpdate(deleteClause, insertClause, whereClause, subjectUri, predicate, oldVar, newFormattedValue, false);
    }

     public void markAnuncioAsInterested(String userUri, String anuncioUri) throws Exception {
        String insertQueryString = String.format("""
            PREFIX cerca: <%s>
            INSERT DATA {
              <%s> cerca:expresaInteres <%s> .
            }
            """, BASE_NAMESPACE, userUri, anuncioUri);
        try {
            executeUpdateQuery(insertQueryString, "markAnuncioAsInterested");
            logger.info("Marcando anuncio {} como interesado para usuario {}", anuncioUri, userUri);
        } catch (Exception e) {
            logger.error("Error al marcar interés por anuncio {} para usuario {}: {}", anuncioUri, userUri, e.getMessage(), e);
            throw e;
        }
    }
    
    public void removeAnuncioInterest(String userUri, String anuncioUri) throws Exception {
        String deleteQueryString = String.format("""
            PREFIX cerca: <%s>
            DELETE DATA {
              <%s> cerca:expresaInteres <%s> .
            }
            """, BASE_NAMESPACE, userUri, anuncioUri);
        try {
            executeUpdateQuery(deleteQueryString, "removeAnuncioInterest");
            logger.info("Quitando interés en anuncio {} para usuario {}", anuncioUri, userUri);
        } catch (Exception e) {
            logger.error("Error al quitar interés por anuncio {} para usuario {}: {}", anuncioUri, userUri, e.getMessage(), e);
            throw e;
        }
    }

    public List<AnuncioSummaryDto> findInterestedAnunciosByUri(String userUri) throws Exception {
         String queryString = String.format("""
            PREFIX rdf: <%s> PREFIX xsd: <%s> PREFIX cerca: <%s>
            SELECT ?anuncioUri ?titulo ?precio ?lat ?lon ?barrio ?tipoVivienda ?fechaCreacionAnuncio ?imagenUrl WHERE {
              BIND(IRI("%s") AS ?user)
              ?user cerca:expresaInteres ?anuncioUri .
              ?anuncioUri rdf:type cerca:Anuncio ; 
                          cerca:tieneTitulo ?titulo ; 
                          cerca:tienePrecioMonto ?precio .
              OPTIONAL { ?anuncioUri cerca:tieneUrlFoto ?imagenUrl . }
              OPTIONAL { 
                ?anuncioUri cerca:describeVivienda ?vivienda . 
                ?vivienda cerca:tieneLatitud ?lat ; 
                          cerca:tieneLongitud ?lon ;
                          cerca:tieneBarrio ?barrio ;
                          cerca:tieneTipoVivienda ?tipoVivienda .
              }
              OPTIONAL { ?anuncioUri cerca:fechaCreacionAnuncio ?fechaCreacionAnuncio .}
            } ORDER BY ?titulo
            """, RDF_NAMESPACE, XSD_NAMESPACE, BASE_NAMESPACE, userUri);

        List<AnuncioSummaryDto> anuncios = new ArrayList<>();
        try {
            List<Map<String, String>> results = executeSelectQuery(queryString, "findInterestedAnunciosByUri");
             for (Map<String, String> sol : results) {
                AnuncioSummaryDto dto = new AnuncioSummaryDto();
                dto.setUri(extractUriValue(sol.get("anuncioUri")));
                dto.setTitulo(extractStringValue(sol.get("titulo")));
                dto.setPrecio(extractDoubleValue(sol.get("precio")));
                dto.setLatitud(extractDoubleValue(sol.get("lat")));
                dto.setLongitud(extractDoubleValue(sol.get("lon")));
                dto.setBarrio(extractStringValue(sol.get("barrio")));
                dto.setTipoVivienda(extractStringValue(sol.get("tipoVivienda")));
                dto.setFechaCreacionAnuncio(extractStringValue(sol.get("fechaCreacionAnuncio")));
                dto.setImagenUrl(extractStringValue(sol.get("imagenUrl")));
                anuncios.add(dto);
            }
        } catch (Exception e) {
            throw e;
        }
        return anuncios;
    }

   public void updateAnuncioState(String anuncioUri, String nuevoEstado) throws Exception {
       String updateQueryString = String.format("""
           PREFIX cerca: <%s>
           PREFIX xsd: <%s>
           DELETE { <%s> cerca:tieneEstadoAnuncio ?estadoActual . }
           INSERT { <%s> cerca:tieneEstadoAnuncio %s . }
           WHERE {
             BIND(IRI("%s") AS ?anuncio)
             OPTIONAL { ?anuncio cerca:tieneEstadoAnuncio ?estadoActual . }
           }
           """,
           BASE_NAMESPACE, XSD_NAMESPACE,
           anuncioUri, anuncioUri, formatXsdString(nuevoEstado), anuncioUri
       );
       try {
           executeUpdateQuery(updateQueryString, "updateAnuncioState");
           logger.info("Actualizando estado de anuncio {} a: {}", anuncioUri, nuevoEstado);
       } catch (Exception e) {
           throw e;
       }
   }

    // --- NUEVOS MÉTODOS PARA ESTADÍSTICAS DE PERFIL ---

    public Optional<Float> findUserAverageRating(String userUri) {
        String queryString = String.format("""
            PREFIX cerca: <%s>
            PREFIX xsd: <%s>
            SELECT ?calificacionPromedio
            WHERE {
                <%s> cerca:tieneCalificacionPromedio ?calificacionPromedio .
            } LIMIT 1
            """, BASE_NAMESPACE, XSD_NAMESPACE, userUri);
        
        try {
            List<Map<String, String>> results = executeSelectQuery(queryString, "findUserAverageRating");
            if (!results.isEmpty()) {
                return Optional.ofNullable(extractFloatValue(results.get(0).get("calificacionPromedio")));
            }
        } catch (Exception e) {
            logger.error("Error obteniendo calificación promedio para {}: {}", userUri, e.getMessage());
        }
        return Optional.empty();
    }

    public int countAnunciosByProviderUri(String providerUri) {
        String queryString = String.format("""
            PREFIX cerca: <%s>
            SELECT (COUNT(DISTINCT ?anuncio) AS ?count)
            WHERE {
                ?anuncio cerca:esPublicadoPor <%s> .
            }
            """, BASE_NAMESPACE, providerUri);
        try {
            List<Map<String, String>> results = executeSelectQuery(queryString, "countAnunciosByProviderUri");
            if (!results.isEmpty() && results.get(0).get("count") != null) {
                return extractIntegerValue(results.get(0).get("count"));
            }
        } catch (Exception e) {
            logger.error("Error contando anuncios para {}: {}", providerUri, e.getMessage());
        }
        return 0;
    }

    public int countInteresesByUserUri(String userUri) {
        String queryString = String.format("""
            PREFIX cerca: <%s>
            SELECT (COUNT(DISTINCT ?anuncio) AS ?count)
            WHERE {
                <%s> cerca:expresaInteres ?anuncio .
            }
            """, BASE_NAMESPACE, userUri);
        try {
            List<Map<String, String>> results = executeSelectQuery(queryString, "countInteresesByUserUri");
            if (!results.isEmpty() && results.get(0).get("count") != null) {
                return extractIntegerValue(results.get(0).get("count"));
            }
        } catch (Exception e) {
            logger.error("Error contando intereses para {}: {}", userUri, e.getMessage());
        }
        return 0;
    }

    // --- FIN DE NUEVOS MÉTODOS ---


    private String extractStringValue(String literalString) {
        if (literalString == null) return null;
        String value = literalString;
        int typeSeparatorIndex = literalString.lastIndexOf("^^");
        if (typeSeparatorIndex != -1) {
            value = literalString.substring(0, typeSeparatorIndex);
        } else {
            int langSeparatorIndex = literalString.lastIndexOf("@");
            if (langSeparatorIndex != -1) {
                if (literalString.substring(langSeparatorIndex + 1).matches("[a-zA-Z]{2,}(-[a-zA-Z0-9]+)*")) {
                     value = literalString.substring(0, langSeparatorIndex);
                }
            }
        }
        
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        
        value = value.replace("\\\"", "\"")
                     .replace("\\\\", "\\")
                     .replace("\\n", "\n")
                     .replace("\\r", "\r")
                     .replace("\\t", "\t");
        return value;
    }

    private Integer extractIntegerValue(String literalString) {
        String valueStr = extractStringValue(literalString);
        if (valueStr == null || valueStr.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(valueStr.trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(valueStr.trim());
            } catch (NumberFormatException nfe) {
                 logger.warn("Could not parse Integer from: {} (original: {})", valueStr, literalString);
                 return null;
            }
        }
    }

    private Double extractDoubleValue(String literalString) {
         String valueStr = extractStringValue(literalString);
         if (valueStr == null || valueStr.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(valueStr.trim().replace(',', '.')); 
        } catch (NumberFormatException e) {
             logger.warn("Could not parse Double from: {} (original: {})", valueStr, literalString);
            return null;
        }
    }

    private Float extractFloatValue(String literalString) {
         String valueStr = extractStringValue(literalString);
         if (valueStr == null || valueStr.trim().isEmpty()) return null;
        try {
             return Float.parseFloat(valueStr.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
             logger.warn("Could not parse Float from: {} (original: {})", valueStr, literalString);
            return null;
        }
    }

    private Boolean extractBooleanValue(String literalString) {
        String valueStr = extractStringValue(literalString);
        if (valueStr == null) return null;
        return "true".equalsIgnoreCase(valueStr.trim()) || "1".equals(valueStr.trim());
    }

    private String extractUriValue(String uriString) {
        if (uriString != null && uriString.startsWith("<") && uriString.endsWith(">")) {
            return uriString.substring(1, uriString.length() -1);
        }
        return uriString;
    }

    private String escapeSparqlLiteral(String literal) {
        if (literal == null) { return ""; } 
        return literal.replace("\\", "\\\\")  
                      .replace("\"", "\\\"")  
                      .replace("\n", "\\n")   
                      .replace("\r", "\\r")   
                      .replace("\t", "\\t");  
    }
}
