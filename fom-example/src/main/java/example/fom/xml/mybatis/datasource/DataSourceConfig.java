package example.fom.xml.mybatis.datasource;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * 
 * @author shanhm
 *
 */
@Configuration
@MapperScan(basePackages="example.fom.xml", sqlSessionTemplateRef="oracleSqlSessionTemplate")
public class DataSourceConfig {

	@Bean("oracleDataSource")
	@ConfigurationProperties(prefix="spring.datasource.hikari.oracal")
	public DataSource dataSource(){
		return DataSourceBuilder.create().build();
	}
	
	@Bean("oracleSqlSessionFactory")
	public SqlSessionFactory sqlSessionFactory(@Qualifier("oracleDataSource") DataSource dataSource) throws Exception{
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/example/fom/xml/**/*Mapper.xml"));
		return factory.getObject();
	}
	
	@Bean("oracleTransactionManager")
	public DataSourceTransactionManager transactionManager(@Qualifier("oracleDataSource") DataSource dataSource){
		return new DataSourceTransactionManager(dataSource);
	}
	
	@Bean("oracleSqlSessionTemplate")
	public SqlSessionTemplate oracleSqlSessionTemplate(@Qualifier("oracleSqlSessionFactory") SqlSessionFactory sqlSessionFactory){
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
