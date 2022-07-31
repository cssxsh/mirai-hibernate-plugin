package xyz.cssxsh.mirai.test;

import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import xyz.cssxsh.mirai.hibernate.MiraiHibernateConfiguration;
import xyz.cssxsh.mirai.test.entry.User;
import xyz.cssxsh.mirai.test.entry.Work;

public class MiraiHibernateDemo extends JavaPlugin {
    public MiraiHibernateDemo() {
        super(new JvmPluginDescriptionBuilder("xyz.cssxsh.mirai.plugin.mirai-hibernate-demo", "0.0.0")
                .dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-demo", false)
                .build());
    }


    @Override
    public void onEnable() {
        // MiraiHibernateConfiguration 会自动扫描 entry, entity, entities, model, models, bean, beans, dto 的包类
        // 只会扫描一个包，先发现先扫
        var configuration = new MiraiHibernateConfiguration(this);
        // 如果你的实体包没被扫描，可以手动指定包扫描
        configuration.scan("xyz.cssxsh.mirai.test.entry");
        // 或者用 Hibernate 原生的方法手动加类
        configuration.addAnnotatedClass(User.class);

        // buildSessionFactory 时会自动建表
        var factory = configuration.buildSessionFactory();

        // 用这个方法能安全关闭 Session 不需要额外写 try catch
        var u = factory.fromSession((session) -> {
            // 查找
            return session.find(User.class, 1L);
        });


        // 用这个方法能安全关闭 Transaction 不需要额外写 try catch
        // fromTransaction 在 fromSession 的基础上套了一层 事务（Transaction）
        // 当有数据变更时，必须套着事务
        factory.fromTransaction((session) -> {
            // 插入
            var user = new User();
            user.setId(1L);
            user.setName("name");
            session.persist(user);

            // 修改
            user.setName("new name");
            session.merge(user);

            // 删除
            session.remove(user);

            // 原生sql 查询
            session.createNativeQuery("select * from user", User.class)
                    .list();

            // hql 查询（格式像是 sql 混杂 java）
            String hql = "from User s where s.name = :name";
            session.createQuery(hql, User.class)
                    .setParameter("name", "...")
                    .list();

            String hql2 = "from Work w where w.user.name = :name";
            session.createQuery(hql2, Work.class)
                    .setParameter("name", "...")
                    .list();

            // criteria 查询（纯 java 代码的方式构造 sql）我推荐这种，不过用起来比较复杂
            var builder = session.getCriteriaBuilder();

            var query = builder.createQuery(User.class);
            var root = query.from(User.class);
            query.select(root);
            query.where(builder.between(root.get("id"), 0L, 1000L));

            var list = session.createQuery(query)
                    .list();

            var query2 = builder.createQuery(Work.class);
            var root2 = query.from(Work.class);
            query2.select(root2);
            query2.where(builder.between(root2.get("user").get("id"), 0L, 1000L));

            var list2 = session.createQuery(query)
                    .list();

            return 0;
        });
    }
}
