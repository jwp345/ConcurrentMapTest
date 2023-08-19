# ConcurrentMapTest
회사에서 겪은 ConcurrentMap 동기화 상황 재연

### 문제 상황
+ redis 안에 데이터를 삭제해도 ConcurrentHashMap에는 반영이 안되는 문제 발생
+ 원인 파악
  + ConcurrentHashMap을 제어하는 클래스는 스케줄러가 있는 클래스에서 putAll()메소드만을 사용해 redis에선 삭제가 일어나도 감지를 못함
+ 기대 상황
  + 스케줄러 메소드가 돌 때 Redis에서 정보를 읽어와 기존에 ConcurrentHashMap에 있던 정보를 clear() 하여 모두 삭제하고 
putAll() 메소드로 새로운 정보를 넣을 때 synchronized()로 다른 클래스에서 이 객체에 대한 접근을 블록 
+ 실행 시
  + 스케줄러 메소드의 종료를 기다리지 않고 블록이 되지 않은 상태에서 객체의 값을 get() 해서 가져감.
+ 기대와 실행이 다른 원인
  + synchronized() 블록 범위의 오해
    + synchronized() 블록으로 객체를 감싼다고 공유 객체에 대한 전역적인 잠금이 이뤄지진 않는다.
    + 해당 메소드 (clearAndPut())에 대한 접근 중 공유 객체에 대한 접근만 블로킹 시킴

+ 테스트
  + 테스트 시나리오 : clear()을 실행 후 일시적으로 Thread를 sleep 시키고 put 메소드를 실행하여 실행이 모두 종료될 때까지 기다리는지 테스트
  + 실패 케이스 : 만약 put메소드가 실행될 때까지 기다리지 않는다면 null을 반환할 것임.
  + 문제 상황
    + 스케줄러 클래스(여기선 스케줄러로 만들지 않음)(ConcurrentHashMap을 제어하는 클래스)
      ![synchronized modifyMap](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/5aeb81f4-f914-4127-8e50-ea0781f45c97)
    
    + 공유 객체(ConcurrentHashMap)에서 get() 실행하는 클래스
      
       ![synchronized getMap](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/e431cbde-0977-4bbf-a8ae-9ae5278ab8d2)
    + 테스트 코드
   
      ![테스트에 사용한 코드](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/b5962913-8288-4d8d-9bf2-d344d64475e7)

    + 결과 : null 반환
   
      ![문제 상황](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/b3510a2b-22eb-4868-b414-5bd2e9750ad8)
      
  + 방법 1 : 공유락 객체 생성으로 인한 동기화 해결
    + 스케줄러 클래스
      
      ![락 공유 modifyMap](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/555b4719-1ea5-440c-b40d-287d623151eb)

    + 공유객체에서 get()을 실행하는 클래스
   
      ![락 공유 getMap](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/893edc4d-7ace-4662-8aa1-c971d0ab8247)

    + 결과 : 성공
   
      ![lock 공유 결과](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/af4038f1-335a-41e5-b303-1cdca0d0bd81)

  + 방법 2 : get 메소드 내에 공유 객체 잠금으로 인한 동기화
    + 공유객체에서 get() 메소드에 synchronized() 블록 생성
      
      ![방법2 getMap synchronized 객체 잠금](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/fe89b7f7-fe2e-4dda-87b2-92e8a4acba09)
    + 결과 : 성공
   
      ![lock 공유 결과](https://github.com/jwp345/ConcurrentMapTest/assets/35333297/af4038f1-335a-41e5-b303-1cdca0d0bd81)

    + 이 방법이 안좋은 이유 : 현재 회사 코드에서 같은 클래스 내부에 ConcurrentHashMap.get() 메소드를 여러 메소드에서 사용하고 있다. 이렇게 사용할 경우, HashTable()객체를 사용하는 것과 다름이 없어짐(여러 스레드에서 조회 할 때 마다 락이 걸림)

### 해결방안
1. 공유 잠금을 생성 하여 스케줄러 메소드가 실행될 때 get이 실행될 때마다 락 획득을 시킬 수 있다.
   + 단점: 모든 get 메소드 마다 락 시도를 해야함 + redis 안에 값이 많아질수록 덮어쓰는 시간이 길어 질수록 성능 저하 발생 + 락 획득 만료 시간을 어디까지 설정해야 하는가의 문제
   + 장점: 동기화로 데이터 일관성 보장
2. concurrentHashMap 안에 있는 값과 Redis에서 읽어온 값을 비교하여 없는 값들만 삭제해준 후 덮어 쓴다.
   + 장점: 성능 저하가 발생하지 않음(ConcurrentHashMap의 get이 CAS 알고리즘으로 동기화를 하지 않기때문에)
   + 단점: 데이터 일관성을 보장하지 못해 일정시간 에러가 발생할 수 있음.
