package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  JPAQueryFactory jpaQueryFactory;


  @BeforeEach
  public void init() {
    /* GIVEN */
    jpaQueryFactory = new JPAQueryFactory(em);

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

  @Test
  @DisplayName("세타조인 - 회원의 이름이 팀 이름과 같은 회원 조회")
  void theta_join() throws Exception {

    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Member> result = jpaQueryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

    for (Member member : result) {
      System.out.println("member = " + member);
    }
  }

  @Test
  @DisplayName("회원과 팀을 조회하면서 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회")
  void join_on_filtering() throws Exception {

    List<Tuple> result = jpaQueryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

  }

  @Test
  @DisplayName("나이가 평균 이상인 회원 구하기")
  void subQueryGoe() throws Exception {

    QMember memberSub = new QMember("memberSub");

    List<Member> result = jpaQueryFactory
            .selectFrom(member)
            .where(member.age.goe(
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            ))
            .fetch();

    assertThat(result).extracting("age")
            .containsExactly(30, 40);

  }

  @Test
  @DisplayName("각 회원의 이름과 전체 회원 평균 나이를 나열")
  void selectSubQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = jpaQueryFactory
            .select(member.username,
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            )
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

  }

  @Test
  void basicCase() throws Exception {
    List<String> result = jpaQueryFactory
            .select(member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("기타")
            )
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }

  }

  @Test
  void complexCase() throws Exception {
    List<String> result = jpaQueryFactory
            .select(new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20살")
                    .when(member.age.between(21, 30)).then("21~30살")
                    .otherwise("기타")
            )
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }

  }

  @Test
  void constant() throws Exception {
    List<Tuple> result = jpaQueryFactory
            .select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

  }

  @Test
  void concat() throws Exception {
    List<String> result = jpaQueryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void memberDToJPQL() throws Exception {

    List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m ", MemberDto.class)
            .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }

  }

  @Test
  void findDtoBySetter() throws Exception {
    List<MemberDto> result = jpaQueryFactory
            .select(Projections.bean(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByField() throws Exception {
    List<MemberDto> result = jpaQueryFactory
            .select(Projections.fields(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByConstructor() throws Exception {
    List<MemberDto> result = jpaQueryFactory
            .select(Projections.constructor(MemberDto.class,
                    member.username,
                    member.age))
            .from(member)
            .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findUserDto() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> result = jpaQueryFactory
            .select(Projections.fields(UserDto.class),
                    member.username.as("name"),
                    ExpressionUtils.as(
                            JPAExpressions
                                    .select(memberSub.age.max())
                                    .from(memberSub), "age")
            )
            .from(member)
            .fetch();

    System.out.println("result = " + result);
  }


  @Test
  public void findDtoByQueryProjection() {
    List<MemberDto> result = jpaQueryFactory
            .select(new QMemberDto(member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameCond, Integer ageCond) {

    BooleanBuilder builder = new BooleanBuilder();
    if (usernameCond != null) {
      builder.and(member.username.eq(usernameCond));
    }
    if (ageCond != null) {
      builder.and(member.age.eq(ageCond));
    }

    return jpaQueryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
  }

  @Test
  public void dynamicQuery_WhereParam() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember2(String usernameCond, Integer ageCond) {

    return jpaQueryFactory
            .selectFrom(member)
//            .where(usernameEq(usernameCond), ageEq(ageCond))
            .where(allEq(usernameCond, ageCond))
            .fetch();
  }

  private BooleanExpression usernameEq(String usernameCond) {
    return usernameCond != null ? member.username.eq(usernameCond) : null;
  }

  private BooleanExpression ageEq(Integer ageCond) {
    return ageCond == null ? member.age.eq(ageCond) : null;
  }

  private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
  }

}
