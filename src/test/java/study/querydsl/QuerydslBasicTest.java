package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory; //JPAQueryFactory는 필드 레벨로 가져가도 된다. Spring framework의 entity manager는 내부적으로 동시성 문제 발생 하지 않도록 설계되어있다.

//    각 테스트 실행 전
    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

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
    public void startJPQL(){
//        member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

//    1.Querydsl 은 컴파일 시점에 오류를 발견할 수 있다
//    2.파라미터 바인딩
    @Test
    public void startQuerydsl(){
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m"); //직접 별칭 만들어 사용

        QMember m = member; //Q 타입의 기본 멤버 사용. static 선언되어있으므로 static import도 가능. static import 하여 QMember m 선언도 필요없어지고, QMember.member -> member로 변경되어 코드 간략해짐

       Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) //파라미터 바인딩
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(
                        member.username.eq("member1") //and의 chain 형식
                                .and(member.age.eq(10)) //마찬가지로 or 도 가능
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where( //and는 chain형식을 유지하지않고 , 로 여러 파라미터를 받을 수 있음
                        member.username.eq("member1"),
                        member.age.eq(10) //마찬가지로 or 도 가능
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne(); //결과 없으면 null, 두건이상이면 NotUniqueResultException 발생

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();//limit(1).fetch(1) 과 결과가 같다

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); //fetchResults는 getTotal(개수), getResult(내용, 컨텐츠)을 제공한다. results.getTotal(); results.getResult(); < 페이징 처리 가능

        long total = queryFactory
                .select(member)
                .fetchCount();

    }


    /*
    * 회원 정렬 순서
    * 1. 회원 나이 내림차순
    * 2. 회원 이름 오름차순
    * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
   * */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작이므로 1로 설정하면 1개를 건너뛴것것
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
   }

    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작이므로 1로 설정하면 1개를 건너뛴것것
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory //querydsl tuple로 반환
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
    * 팀의 이름과 각 팀의 평균 연령을 구해라
    * */
    @Test
    public void group(){
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
//                .having(team.name.eq("teamA"))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /*
    * 팀 A에 소속된 모든 회원원
   * */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //member.team과 Qteam.team을 조인 / 기본적으로 join은 inner join
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
    * 연관관계 없는 경우
    * 세타 조인
    * 회원의 이름이 팀 이름과 같은 회원 조회
    * */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //join이 아니라 from절에서 여러 테이블을 가져온뒤 where로 매칭
                .where(member.username.eq(team.name)) // 이 방식으로는 외부조인(left outer, right outer join이 불가능). -> on 을 사용하여 외부 조인 가능
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    * 회원과 팀을 조인하면서 팀이름이 teamA인 팀만 조인, 회원은 모두 조회
    * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
    * */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple: result){
            System.out.println("tuple = "+ tuple);
        }
    }

    /*
    * 연관관계 없는 엔티티 외부 조인
    * 회원의 이름이 팀 이름과 같은 대상 외부 조인
    * */
    @Test
    public void join_on_no_relation(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple: result){
            System.out.println("tuple = "+ tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne(); //member와 team은 LAZY 설정이므로 team이 조회되지 않는다

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//member의 team이 로딩된상태인지 아직 로딩안된상태인지 확인

        assertThat(loaded).as("페치 조인 미적용").isFalse(); // Lazy상태에 fetch join을 안했으므로 False가 나와야함
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 그냥 join이랑 같은데 뒤에 fetchJoin()만 붙여주면된다.
                .where(member.username.eq("member1"))
                .fetchOne(); //member와 team은 LAZY 설정이지만 fetch조인했으므로 team이 조회된다

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//member의 team이 로딩된상태인지 아직 로딩안된상태인지 확인

        assertThat(loaded).as("페치 조인 미적용").isTrue(); // Lazy상태에 fetch join을 했으므로 True가 나와야함
    }

    //subQuery는 JPAExpressions 사용
    @Test
    public void subQuery(){
        //Alias 중복 방지를 위해 새로운 이름으로 생성
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    @Test
    public void subQueryGoe(){
        //Alias 중복 방지를 위해 새로운 이름으로 생성
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void subQueryIn(){
        //Alias 중복 방지를 위해 새로운 이름으로 생성
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username, select(memberSub.age.avg())
                        .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory.select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) //실제 쿼리상에서는 A가 나가지않고 어플리케이션단에서 만들어짐
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }

    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

//    프로젝션 : select 대상 지정
//    프로젝션 대상이 하나인 경우 타입을 명확하게 지정가능 List<T>
//    프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch(); //반환타입이 Tuple
//         주의. import문을 보면 위에 사용된 Tuple은 querydsl 에 종속적이므로 querydsl.tuple은 repository 레벨에서만 사용하고
//        service로 리턴할 때는 DTO 로 변환시켜 return 하는 것을 권장한다.

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL(){
//        순수 JPA에서 DTO를 조회할 떄는 new 명령어를 사용해야하고, 생성자 방식만 지원한다.
        //JPQL의 경우 타입을 맞추기 위해 new study.querydsl.dto.MemberDto(m.username, m.age) 로  하는것(패키지명도 다 적어야함)이 예쁘지않음.
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
//    Querydsl의 경우 프로퍼티접근(setter), 필드 직접 접근, 생성자 사용 . 이렇게 3가지 방식을 지원한다.
    @Test
    public void findDtoBySetter(){
        //setter를 통해 값 주입
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField(){
        //setter 사용하지않고 필드에 값을 바로 꽂는 방식
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    //    별첨이 다른 경우. UserDto는 username이 아닌 String name;으로 설정.
    @Test
    public void findUserDtoByFields(){
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class
                        , member.username.as("name") // 이름을 맞춰줘야한다.
                        , member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

//    생성자는 필드명이 아닌 타입으로 가기 때문에 UserDto로 바꿔도 따로 as 처리를 해주지 않아도된다.
    @Test
    public void findDtoByConstructor(){

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


//    subquery를 가져와서 DTO에 넣을때도 subquery에 별칭을 짓는 과정이 필요하다
    @Test
    public void findSubQueryUserDtoByConstructor(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class
                        , member.username.as("name") // 이름을 맞춰줘야한다.
                        , ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }


}




















