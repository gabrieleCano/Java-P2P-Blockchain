package org.sharpei.utils;

import com.google.gson.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.sharpei.blockchain.Block;
import org.sharpei.blockchain.Blockchain;
import org.sharpei.blockchain.Utxo;
import org.sharpei.blockchain.Wallet;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.security.PublicKey;

public class GsonUtils {

    private static final Gson gsonBlock = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayTypeAdapter())
            .registerTypeHierarchyAdapter(Wallet.class, new WalletTypeAdapter())
            .registerTypeHierarchyAdapter(PublicKey.class, new PublicKeyTypeAdapter())
            .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            .excludeFieldsWithoutExposeAnnotation()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final Gson gsonBlockchain = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayTypeAdapter())
            .registerTypeHierarchyAdapter(Wallet.class, new WalletTypeAdapter())
            .registerTypeHierarchyAdapter(PublicKey.class, new PublicKeyTypeAdapter())
            .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            .excludeFieldsWithoutExposeAnnotation()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final Gson gsonUtxo = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayTypeAdapter())
            .registerTypeHierarchyAdapter(PublicKey.class, new PublicKeyTypeAdapter())
            .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            .excludeFieldsWithoutExposeAnnotation()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();


    private static class ByteArrayTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.getAsString().getBytes();
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(StringUtils.newStringUtf8(src));
        }
    }

    private static class WalletTypeAdapter implements JsonSerializer<Wallet>, JsonDeserializer<Wallet> {
        public Wallet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return null;
        }

        public JsonElement serialize(Wallet src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Hex.encodeHexString(src.getPublicKey().getEncoded()));
        }
    }

    private static class PublicKeyTypeAdapter implements JsonSerializer<PublicKey>, JsonDeserializer<PublicKey> {
        public PublicKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return null;
        }

        public JsonElement serialize(PublicKey src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Hex.encodeHexString(src.getEncoded()));
        }
    }

    public static String printBlock(Block block) {
        return gsonBlock.toJson(block);
    }

    public static JsonElement blockToJsonElement(Block block) {
        return gsonBlock.toJsonTree(block);
    }

    public static String printUtxo(Utxo utxo) {
        return gsonUtxo.toJson(utxo);
    }

    public static String printBlockchain(Blockchain blockchain) {

        try (Writer writer = new FileWriter("Blockchain.json")) {
            gsonBlockchain.toJson(blockchain.getJson(), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return gsonBlockchain.toJson(blockchain.getJson());
    }
}
