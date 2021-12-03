package study.querydsl.entity;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

  @BeforeEach
  public void init() {
    /* GIVEN */
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);

    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

    em.flush();
    em.clear();

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
  }

  @Test
  void startJPQL() throws Exception {
    /* GIVEN */
    //member1을 찾아라
    Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

    /* WHEN */

    /* THEN */
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQuerydsl() throws Exception {
    /* GIVEN */
    Member findMember = jpaQueryFactory
            .select(member)
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();

    /* WHEN */

    /* THEN */
    assertThat(findMember.getUsername()).isEqualTo("member1");

  }

  @Test
  void search() throws Exception {
    /* GIVEN */
    Member findMember = jpaQueryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                    .and(member.age.eq(10)))
            .fetchOne();

    /* WHEN */

    /* THEN */
    assertThat(findMember.getUsername()).isEqualTo("member1");

  }
}
