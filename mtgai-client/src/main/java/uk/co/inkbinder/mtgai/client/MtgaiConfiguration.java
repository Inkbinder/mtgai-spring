package uk.co.inkbinder.mtgai.client;

import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Filter;

@Configuration
@Slf4j
public class MtgaiConfiguration {

    @Value("classpath:mtg_comprehensive_rules.txt")
    Resource resourceFile;

    private void loadVectorStore(ChromaVectorStore chromaVectorStore) {
        try {
            log.info("Loading vector store");

            String[] splits = (new DocumentByLineSplitter(1000, 200)).split(resourceFile.getContentAsString(StandardCharsets.UTF_8));
            int chunkSize = 50;
            for (int i = 0; i < splits.length; i += chunkSize) {
                int end = Math.min(splits.length, i + chunkSize); // Handle cases where the final chunk is smaller
                String[] chunk = Arrays.copyOfRange(splits, i, end);
                chromaVectorStore.add(Arrays.stream(chunk).map(Document::new).toList());
                // Process each chunk
                log.info("Processing chunk: {}", i);
            }
        } catch (Exception e) {
            log.error("Failed to load vector store", e);
        }

    }

    @Bean
    public ChromaVectorStore vectorStore(OllamaEmbeddingModel embeddingModel, ChromaApi chromaApi) {
        ChromaVectorStore chromaVectorStore = ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName("mtg-ai")
                .databaseName("mtg-rules")
                .collectionName("mtg-comprehensive-rules")
                .initializeSchema(true)
                .initializeImmediately(true)
                .build();


        ChromaApi.Collection collection = chromaApi.getCollection("mtg-ai", "mtg-rules", "mtg-comprehensive-rules");
        if (collection == null || chromaApi.countEmbeddings("mtg-ai", "mtg-rules", collection.id()) == 0){
            loadVectorStore(chromaVectorStore);
            ChromaApi.Collection col2 = chromaApi.getCollection("mtg-ai", "mtg-rules", "mtg-comprehensive-rules");
            log.info("Loaded vector store {}", chromaApi.countEmbeddings("mtg-ai", "mtg-rules", col2.id()));
        }
        return chromaVectorStore;
    }


}
