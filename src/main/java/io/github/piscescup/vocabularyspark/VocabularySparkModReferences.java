package io.github.piscescup.vocabularyspark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Constants
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class VocabularySparkModReferences {
    /** The mod identifier, also used as the namespace for mod resources. */
    public static final String MOD_ID = "vocabulary-spark";

    /** The Fabric loader instance providing access to registered mods. */
    public static final FabricLoader FABRIC_LOADER = FabricLoader.getInstance();

    /** The registered container for the mod identified by {@link #MOD_ID}. */
    public static final ModContainer VOCABULARY_SPARK_MOD = FABRIC_LOADER
        .getModContainer(MOD_ID)
        .orElseThrow();

    /** The metadata supplied by {@link #VOCABULARY_SPARK_MOD}. */
    public static final ModMetadata VOCABULARY_SPARK_METADATA = VOCABULARY_SPARK_MOD
        .getMetadata();

    /** The human-readable mod name obtained from the registered metadata. */
    public static final String MOD_NAME = VOCABULARY_SPARK_METADATA
        .getName();

    /** The friendly version string obtained from the registered metadata. */
    public static final String MOD_VERSION = VOCABULARY_SPARK_METADATA
        .getVersion()
        .getFriendlyString();

    /**
     * The shared Gson instance for JSON serialization and deserialization.
     * Uses default Gson mappings with pretty printing enabled for JSON output.
     */
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /** The shared Log4j logger named using {@link #MOD_NAME}. */
    public static final Logger MOD_LOGGER = LogManager.getLogger(MOD_NAME);

    /**
     * Creates a resource identifier using {@link #MOD_ID} as its namespace.
     * For example, {@code "textures/gui/dictionary.png"} produces
     * {@code "vocabulary-spark:textures/gui/dictionary.png"}.
     *
     * <p>The path is passed unchanged to
     * {@link Identifier#fromNamespaceAndPath(String, String)}. It must contain
     * only lowercase ASCII letters, digits, slashes, periods, underscores or
     * hyphens; a namespace prefix must not be included.
     *
     * @param path the non-null resource path within the mod's namespace
     * @return a new identifier with the mod namespace and the supplied path
     * @throws NullPointerException if {@code path} is null
     * @throws net.minecraft.IdentifierException if {@code path} contains an
     *         invalid resource-path character
     * @see Identifier#fromNamespaceAndPath(String, String)
     */
    @NonNull
    @Contract("_ -> new")
    public static Identifier fromPath(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
