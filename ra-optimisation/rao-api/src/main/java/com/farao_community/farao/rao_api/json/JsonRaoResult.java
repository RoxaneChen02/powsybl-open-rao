/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A class to import and export Rao Results in a Json format
 *
 * @author Philippe Edwards <philippe.edwards at rte-france.com>
 */
public final class JsonRaoResult {

    private JsonRaoResult() { }

    /**
     * A configuration loader interface for the RaoResult extensions loaded
     *
     * @param <E> The extension class
     */
    public interface ExtensionSerializer<E extends Extension<RaoResult>> extends ExtensionJsonSerializer<RaoResult, E> {
    }

    /**
     * Lazily initialized list of extension serializers.
     */
    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "rao-result"));

    /**
     * Gets the known extension serializers.
     */
    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    /**
     * Reads result from a JSON file (will NOT rely on platform config).
     */
    public static RaoResult read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads result from a JSON file (will NOT rely on platform config).
     */
    public static RaoResult read(InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerFor(RaoResult.class).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes result as JSON to a file.
     */
    public static void write(RaoResult result, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(result, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes result as JSON to an output stream.
     */
    public static void write(RaoResult result, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Low level deserialization method, to be used for instance for reading rao computation parameters nested in another object.
     */
    public static RaoResult deserialize(JsonParser parser, DeserializationContext context, RaoResult parameters) throws IOException {
        return new RaoResultDeserializer().deserialize(parser, context, parameters);
    }

    /**
     * Low level deserialization method, to be used for instance for updating rao computation parameters nested in another object.
     */
    public static RaoResult deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return new RaoResultDeserializer().deserialize(parser, context);
    }

    /**
     * Low level serialization method, to be used for instance for writing rao computation parameters nested in another object.
     */
    public static void serialize(RaoResult parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        new RaoResultSerializer().serialize(parameters, jsonGenerator, serializerProvider);
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
            .registerModule(new RaoResultJsonModule());
    }
}