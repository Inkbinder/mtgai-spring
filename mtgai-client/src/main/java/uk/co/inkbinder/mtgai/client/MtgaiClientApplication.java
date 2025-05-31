package uk.co.inkbinder.mtgai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
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
		return args -> {
			ChatClient chatClient = builder.build();
						String response = chatClient.prompt().system("""							
					You are an expert on Magic: The Gathering rules.
					Answer the following question based on the provided context from the MTG Comprehensive Rules.
					If the answer cannot be found in the context, say "I don't have enough information to answer that question."
""")
								.advisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().similarityThreshold(0.3).topK(4).build()).build())
								.user("Explain first strike").call().content();
			System.out.println(response);
		};
	}

}
