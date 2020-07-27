package example.fom.fomschedulbatch.mybatis.datasource;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
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
@Configuration("mysqlDataSourceConfig")
public class DataSourceConfig {

	@Bean("mysqlDataSource")
	@ConfigurationProperties(prefix="spring.datasource.hikari.mysql")
	public DataSource dataSource(){
		return DataSourceBuilder.create().build();
	}
	
	@Bean("mysqlSqlSessionFactory")
	public SqlSessionFactory sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception{
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/example/fom/fomschedulbatch/**/*Mapper.xml"));
		return factory.getObject();
	}
	
	@Bean("mysqlTransactionManager")
	public DataSourceTransactionManager transactionManager(@Qualifier("mysqlDataSource") DataSource dataSource){
		return new DataSourceTransactionManager(dataSource);
	}
	
	@Bean("mysqlSqlSessionTemplate")
	public SqlSessionTemplate oracleSqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory){
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}


