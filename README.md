Transaction batch process
1.	INTRODUCTION

  This project is a small demo of a high-perfomance massive data transactions from excel files and transfer data between tables in oracle Database. It include logic to detect data formats, find duplicate trace within a batch, catch errors and provide full upload batch results.
  It is mainly design to handle up to 10 excel files at once, each file 1 million row and sum up to 10 million per batch upload. Due to using JPA to do this project so the result is medium speed but i personally feel it process really fast. The design ensures ACID compliance and focuses on safe data insertion, automatic recovery if server crashes in middle of processing. 

2.	FEATURES AND TECH HIGHTLIGHT
   
  High-performance batch process: using full JPA with Oracle Database but it got optimized to at least match the requirement speed of processing. Instead of save row by row or save all in once, this system utilized chunk process and bulk insert to scale and handle the massive data. It is capable of uploading 10 excel files with sum up 10 million of records and approving 100 million rows to transfer data from table to table in the database.
  User-role and security: Enforces strict role-based for user(ADMIN, UPLOADER, APPROVER), many services require user-role in order to use them and uses JWT to maintain user state.
  Data validation and duplicate prevention: Check data formats and prevent duplicate traces within a batch before insert them into table during the file upload process. When approving to transfer data to the main table, it will check and mark those trace that already exists in main table as status ‘DEL’ in order to optimize insert function and prevent duplicate. 
  Auto self-recorvery: if the server crashes, an automatic self recovery function with scheduled task will recover the stuck batches with status ‘PROCESSING’ and ‘APPROVING’ if they are processing for too long.
  Asynchronous internal processing: when a user uploads files, the system pushes them into queues then creates and copies them to a secure folder (with logic to prevent path traversal attacks). Because it run asynchronously, user can don’t have to wait for the system to finish processing, they can do other things while waiting. 

3.	TECH STACK 
    + Backend framework: Java 21, Spring boot.
    + Security: Spring security & JWT Authentication.
    + Data Access: Spring data JPA, Hibernate, Native SQL queries.
    + Database: Oracle database.
    + File processor: Apache POI.   

4.	CORE ARCHITECTURE AND LOGIC FLOW
     
    4.1. Upload Phase
    When an UPLOADER submits a batch upload of excel files: 
    + The file is parsed to chunk in order to optimize RAM usage.
    + All the files from that batch will be store in a secure folder. With this system can makes queues and secures the process, avoiding the server crashes or memory overloads from remember all files at once. 
    + For every transaction row from excel file that got inserted into database, they are match the requirement and each will got status ‘INIT’ waiting for approval.
    + The batch will got status as ‘PENDING’ in table (UPLOAD_BATCH) when user just uploads them. when the system starting to process them, it will be locks and marks status as ‘PROCESSING’, when it done it will be ‘COMPLETED’ and wait for approval .

    4.2. Approval phase
    When an APPROVER reviews a batch or submits a date range in order to select all the batches in that range and click approve it, the system will execute them by update-insert-update strategy:
    + Marked and locked all the batch that got selected with status ‘COMPLETED’ change status to ‘APPROVING’, when system done processing all the transactions of a batch, change it status to ‘APPROVED’.
    + Using INSERT ... SELECT ... EXIST in order to mark all the duplicate rows have same traces in staging table(MB_TRANSACTION_UPL) that alreay existed in the core table (MB_TRANSACTION) to status ‘DEL’.
    + After mark all the invalid rows with status ‘DEL’ it mean it got rejected, the system will insert all the ‘INIT’ rows left with INSERT ... SELECT to core table change status to ‘ACTIVE’. 
    + After insert successfully, update all the row that got inserted as status ‘ACTIVE’ in staging table and it done. 

5.	API ENDPOINT

Here are list of core API in this project, system. All of them got secured with JWT and required authentication to access and use them.
  
  Upload & View (Role: UPLOADER, ADMIN)
  + POST /api/upload-batches: Upload one or multiple Excel files. The system will process them asynchronously and internally.
  + GET /api/upload-batches/my: Retrieve all the batches history with result detail uploaded by current user.  
  
  Search & Monitor (Role: UPLOADER, APPROVER, ADMIN)
  + GET /api/upload-batches/{batchId}: Selected a single batch to see full result and detail of that batch 
  
  Approval operations (Role: APPROVER, ADMIN)
  + GET /api/upload-batches?start={date}&end={date}&status={status}: search list of batches with date range. Status is not required but if add status to it, it will filter all batches with that status.
  + POST /api/upload-batches/{batchId}/approve: Review and approve a single batch that got selected. 
  + POST /api/upload-batches/batches/approve: Bulk approve list of batches with status ‘COMPLETED’ by provide date range to the requested.
  
  System recovery (Role: ADMIN)
  + POST /api/upload-batches/recover-processing-batches: Trigger self recovery service, it will scan the database to see if any batch that got processing for too long with status ‘PROCESSING’. 
  + POST /api/upload-batches/recover-approving-batches: Trigger self recovery service, it will scan the database to see if any batch that got approving for too long with status 'APPROVING'

6.	HOW TO RUN THE PROJECT

    Step 1: Git clone to your folder you want first.
    Step 2: Open ORACLE_DB file and copy paste them to your Oracle Database and run all of them to create all tables. 
    Step 3: Open propertiesForThisProject file and copy paste them replace your properties file and remember, input your Oracle Database “Username” and “Password” in the properties file.
    Step 4: Open Eclipse or Intellij IDEA, open terminal run “mvn clean install” to install all necessary dependencies and run “mvn spring-boot:run” and it done.
