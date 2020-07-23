package org.eto.fom.boot;

import java.io.File;

import javax.servlet.MultipartConfigElement;

import org.eto.fom.boot.listener.FomListener;
import org.eto.fom.boot.listener.PoolListener;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 
 * @author shanhm
 *
 */
@Configuration
public class FomConfiguration  {
	
	@Bean
	public ServletListenerRegistrationBean<PoolListener> listenPool(){
		ServletListenerRegistrationBean<PoolListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new PoolListener());
		listener.setOrder(4);
		return listener;
	}

	@Bean
	public ServletListenerRegistrationBean<FomListener> listenConfig(){
		ServletListenerRegistrationBean<FomListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new FomListener());
		listener.setOrder(5);
		return listener;
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", buildConfig()); 
		return new CorsFilter(source);
	}
	
	//跨域问题
	@Bean
	public CorsConfiguration buildConfig() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin("*");
		corsConfiguration.addAllowedHeader("*");
		corsConfiguration.addAllowedMethod("*");
		corsConfiguration.setAllowCredentials(true);//这两句不加不能跨域上传文件，
		corsConfiguration.setMaxAge(3600l);//加上去就可以了
		return corsConfiguration;
	}
	
	//文件上传问题
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        String location = System.getProperty("user.dir") + "/data/tmp";
        File tmpFile = new File(location);
        if (!tmpFile.exists()) {
            tmpFile.mkdirs();
        }
        factory.setLocation(location);
        return factory.createMultipartConfig();
    }
}
