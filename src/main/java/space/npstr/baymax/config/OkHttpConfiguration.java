package space.npstr.baymax.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 05.09.18.
 */
@Configuration
public class OkHttpConfiguration {

    //a general purpose http client builder
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) //do not reuse the builders
    public static OkHttpClient.Builder httpClientBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
    }

    // default http client that can be used for anything
    @Bean
    public OkHttpClient defaultHttpClient(OkHttpClient.Builder httpClientBuilder) {
        return httpClientBuilder.build();
    }
}
