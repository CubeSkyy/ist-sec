# Dependable Public Announcement Server

Project for 2019-2020 Highly Dependable Systems Course in Instituto Superior Técnico.

### Built With

* [Maven](https://maven.apache.org/) - Dependency Management
* [gRPC](https://grpc.io/docs/) - Client/Server RPC Architecture.
* [JUnit](https://junit.org/junit4/) - For unit testing

### Installing
In the base project directory:
```
mvn compile
```

### Running the tests
In the base project directory:
```
mvn test
```

### Executing Client and Server
In the server directory:
```
mvn exec:java
```

In the client directory:
```
mvn exec:java
```

### Client Command Usage

#### Register:

Registers a user in the server.
```
register|<userAlias>
```
* userAlias - Alias of the user to be registered

#### Post:

Posts a message in the user's personal board.
```
post|<userAlias>|<Message>
```
or with references
```
post|<userAlias>|<Message>|<Ref1>|<Ref2>|...
```
* userAlias - Alias of the user that is posting
* Message - Message of post
* RefN- Post Ids that refer to other existing posts

#### PostGeneral:

Posts a message in the general board.
```
postGeneral|<userAlias>|<Message>
```
or with references
```
postGeneral|<userAlias>|<Message>|<Ref1>|<Ref2>|...
```
* userAlias - Alias of the user that is posting
* Message - Message of post
* RefN- Post Ids that refer to other existing posts

#### Read:

Reads from some user's personal board.
```
read|<userAlias>|<userToRead>|<NumberOfPosts>
```
* userAlias - Alias of the user that is posting
* userToRead - Owner of the board we want to read
* NumberOfPosts- Number of posts to read. (0 reads all)

#### ReadGeneral:

Reads from the General board.

```
readGeneral|<userAlias>|<NumberOfPosts>
```
* userAlias - Alias of the user that is posting
* NumberOfPosts- Number of posts to read. (0 reads all)

### Running demos:

Start the server.
Start the client.
Execute like other commands:

#### Demo 1:

```
demo1
```
* register | user1
* post | user1 | Test
* read | user1 | user1 | 0

#### Demo 2:

```
demo2
```
* register | user1
* register | user2
* postGeneral | user1 | Test
* postGeneral | user2 | Test2 | 1
* readGeneral | user1 | 0

#### Demo 3:

```
demo3
```
* register | user1
* post | user1 | Test
* post | user1 | Test2
* post | user1 | Test3
* read | user1 | user1 | 2

## Authors

* **Mara  Caldeira** - *83506*
* **Miguel Coelho** - *87687*
* **Pedro Alves** - *87692*

