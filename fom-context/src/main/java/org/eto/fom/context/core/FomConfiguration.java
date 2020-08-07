package org.eto.fom.context.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eto.fom.context.annotation.FomScan;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 
 * @author shanhm
 *
 */
@Configuration
@Import(FomConfiguration.class)
public class FomConfiguration implements ImportBeanDefinitionRegistrar{
	
	static List<String> packages = new ArrayList<>();

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		AnnotationAttributes attrs = 
				AnnotationAttributes.fromMap(meta.getAnnotationAttributes(FomScan.class.getName()));
		if(attrs == null){
			return;
		}
		
		String[] basePackages = (String[])attrs.get("basePackages");
		packages.addAll(Arrays.asList(basePackages));
	}

}
