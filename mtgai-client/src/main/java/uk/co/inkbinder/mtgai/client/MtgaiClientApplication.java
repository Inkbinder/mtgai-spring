package uk.co.inkbinder.mtgai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication

public class MtgaiClientApplication {

	@Autowired
	VectorStore vectorStore;

	public static void main(String[] args) {
		SpringApplication.run(MtgaiClientApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(ChatClient.Builder builder) {

		PromptTemplate customPromptTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.template("""
            <query>

            Context information is below.

			---------------------
			<question_answer_context>
			---------------------
			
			You are an expert on Magic: The Gathering rules.
			Given the context information and no prior knowledge, answer the query.

			Follow these rules:

			1. If the answer is not in the context, just say that you don't know.
			2. Avoid statements like "Based on the context..." or "The provided information...".
            """)
				.build();


		return args -> {
			ChatClient chatClient = builder.build();
						String response = chatClient.prompt("Explain first strike")
								.advisors(RetrievalAugmentationAdvisor.builder().documentRetriever(
												VectorStoreDocumentRetriever.builder().similarityThreshold(0.30)
														.vectorStore(vectorStore).build()
										).build())
								.call().content();
			System.out.println(response);
		};
	}

}
