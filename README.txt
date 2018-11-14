## 该文件用来存放spring 整合过程中涉及到的注意事项，比如jar包依赖，属性配置，编程配置等

## 集成SpringMVC
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>


## 集成lombok
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    说明：用来简化类，常用注解有：
    @Data 注解在类上；提供类所有属性的 getting 和 setting 方法，此外还提供了equals、canEqual、hashCode、toString 方法
    @Setter ：注解在属性上；为属性提供 setting 方法
    @Setter ：注解在属性上；为属性提供 getting 方法
    @Log4j/@Slf4j ：注解在类上；为类提供一个 属性名为log 的 log4j 日志对象
    @NoArgsConstructor ：注解在类上；为类提供一个无参的构造方法
    @AllArgsConstructor ：注解在类上；为类提供一个全参的构造方法
    @Cleanup : 可以关闭流
    @Builder ： 被注解的类加个构造者模式
    @Synchronized ： 加个同步锁
    @SneakyThrows : 等同于try/catch 捕获异常
    @NonNull : 如果给参数加个这个注解 参数为null会抛出空指针异常
    @Value : 注解和@Data类似，区别在于它会把所有成员变量默认定义为private final修饰，并且不会生成set方法。


## logback配置
    SpringBoot已经集成logback，只需配置logback-spring.xml 文件即可
    logback详细配置见配置文件，需要注意的是要在文件中指定一下日志存放根路径logging.path
    若配置文件在resources 根目录下则无需在application.properties 文件中指定，否则需要做如下配置：
    logging.config=classpath:logback-spring.xml
    logback 可以与lombok 配合使用，使用@Slf4j 注解可以使用log 对象进行日志输出


## 集成MyBatis 以及配置代码自动生成
    1、引入依赖
    <!-- MyBatis及自动代码生成 -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>1.3.1</version>
    </dependency>
    <dependency><!-- alibaba 的druid数据库连接池，若报错检查版本是否有问题 -->
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
        <version>1.1.9</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    <dependency><!--mybatis 自动生成插件-->
        <groupId>org.mybatis.generator</groupId>
        <artifactId>mybatis-generator-core</artifactId>
        <version>1.3.5</version>
    </dependency>

    <!-- mybatis generator 自动生成代码插件 -->
    <!-- 在项目根目录下cmd运行命令: mvn mybatis-generator:generate -e -->
    <plugin>
        <groupId>org.mybatis.generator</groupId>
        <artifactId>mybatis-generator-maven-plugin</artifactId>
        <version>1.3.2</version>
        <configuration>
            <configurationFile>${basedir}/src/main/resources/mybatis/mybatis-generator-config.xml</configurationFile>
            <overwrite>true</overwrite>
            <verbose>true</verbose>
        </configuration>
    </plugin>

    2、编辑generator文件
       mybatis/mybatis-generator-config.xml
       设置启动命令（Idea）：
       Run -> Edit Configurations -> + -> Maven -> Command line -> mybatis-generator:generate -e

    3、数据库配置
        spring:
            datasource:
                name: epay
                type: com.alibaba.druid.pool.DruidDataSource
                druid:
                  driver-class-name: com.mysql.jdbc.Driver
                  url: jdbc:mysql://127.0.0.1:3306/epay
                  username: root
                  password: root

    4、mybatis配置
        ## 该配置节点为独立的节点，有很多同学容易将这个配置放在spring的节点下，导致配置无法被识别
        mybatis:
            # 注意：一定要对应mapper映射xml文件的所在路径
            mapper-locations: classpath:mybatis/mapper/*.xml
            # 注意：对应实体类的路径
            type-aliases-package: com.example.demo.dao.entity

    5、在启动类上添加 @MapperScan("com.example.demo.dao.mapper")
    
## 集成分页
    <!-- 分页依赖 -->
    <dependency>
        <groupId>com.github.pagehelper</groupId>
        <artifactId>pagehelper-spring-boot-starter</artifactId>
        <version>1.2.3</version>
    </dependency>
    
    #分页的配置
    pagehelper:
      offset-as-page-num: true
      row-bounds-with-count: true
      reasonable: true
      
    #在service 中使用
    public List<EpayOrder> queryOrderByPage() {
        // 设置PageNum、PageSize
        PageHelper.startPage(1,2);
        // 当前list 会被自动封装进Page，不要直接return 查询语句，否则分页会失效
        List<EpayOrder> list = epayOrderMapper.selectEpayOrder();
        return list;
    }
    
## 集成 dubbo
    1、服务提供者
    1）引入依赖：当前jar包已包含zookeeper client 无需引入
    <dependency>
    	<groupId>com.alibaba.boot</groupId>
    	<artifactId>dubbo-spring-boot-starter</artifactId>
    	<version>0.2.0</version>
    </dependency>
    
    2）dubbo 属性配置
        dubbo:
          application:
            name: dubboTest
          protocol:
            name: dubbo
            port: 20880
          registry:
            address: zookeeper://localhost:2181 # 注册中心地址
          consumer: # 在服务端设置消费端的调用规则
            timeout: 5000 # 超时时间ms
            retries: 2  # 重试次数
            loadbalance: roundrobin # 负载均衡算法，缺省是随机 random。还可以有轮询 roundrobin、最不活跃优先 leastactive
    
    3）在启动类上使用@EnableDubbo 启动dubbo
    4）通过实现接口的方式定义服务，使用阿里的@Service 注解标注要对外曝露的服务
    import com.alibaba.dubbo.config.annotation.Service;
    
    5）打包服务的接口、模型应用提供给服务消费方依赖
    
    2、服务消费者
    1）引入dubbo依赖，同时引入服务提供方接口依赖
    <dependency>
    	<groupId>com.eden</groupId>
    	<artifactId>dubbo-api</artifactId>
    	<version>1.0-SNAPSHOT</version>
    </dependency>
    
    2）dubbo 外部配置同上
    3）在启动类上使用@EnableDubbo 启动dubbo
    4) 使用阿里@Service 注解将服务注册到注册中心
    5）使用阿里@Reference 注解从注册中心获取服务
    6）管理控制台安装
    http://dubbo.apache.org/zh-cn/docs/admin/install/admin-console.html
	
## 集成redis
	1、引入依赖
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-data-redis</artifactId>
	</dependency>
	<!--spring2.0集成redis所需common-pool2-->
	<dependency>
		<groupId>org.apache.commons</groupId>
		<artifactId>commons-pool2</artifactId>
		<version>2.4.2</version>
	</dependency>
	<!-- 将作为Redis对象序列化器 -->
	<dependency>
		<groupId>com.alibaba</groupId>
		<artifactId>fastjson</artifactId>
		<version>1.2.47</version>
	</dependency>
	
	2、属性配置：application.yml 
	spring:
	  redis:
		database: 0
		host: 192.168.0.103
		port: 6379
		password:
		timeout: 10000 # 连接超时时间
		jedis:
		  pool:
			max-active: 8 # 最大连接数
			max-wait: -1  # 最大阻塞等待时间（负数表示没有限制）
			max-idle: 5 # 最大空闲连接数
			min-idle: 0 # 最小空闲连接数
			
	3、Redis自定义配置类：com.eden.config.RedisConfig
	    1）自定义序列化器
	    2）自定义缓存配置
	4、Redis工具类：com.eden.utils.RedisUtils
	
## 集成事物 @Transaction
    1、事物分类：声明式事物、编程式事物
    2、原理：Spring Aop 的环绕通知来实现
    3、注意：Springboot中默认开启，使用时业务代码不要处理异常直接抛出                                                                                                                                                                                             

## 集成AOP
    1、引入依赖
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    2、开启切面自动代理：@EnableAspectJAutoProxy
    3、定义切面：@Aspect、@PointCut、@Before、@After、@Around、@AfterReturning、@AfterThrowing