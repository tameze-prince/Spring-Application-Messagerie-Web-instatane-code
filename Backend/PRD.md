PRD — Application de Messagerie Web
Telegram-Inspired Real-Time Messaging Platform
Version : 1.0
Statut : Product Requirements Document
Plateforme initiale : Web
Architecture cible : Scalable Modular Monolith
Temps réel : WebSocket
Base de données : PostgreSQL
Cache / présence : Redis
Stockage fichiers : S3-compatible Object Storage
1. Vision du produit
Créer une application Web de messagerie moderne permettant aux utilisateurs de :
• discuter en privé ;
• créer des groupes ;
• créer et suivre des canaux ;
• envoyer des messages texte, images, vidéos, documents et fichiers ;
• répondre et transférer des messages ;
• rechercher dans les conversations ;
• partager des médias ;
• gérer plusieurs appareils/sessions ;
• recevoir les messages en temps réel ;
• recevoir des notifications ;
• gérer leur profil et leurs paramètres de confidentialité.
La philosophie produit est inspirée des forces de Telegram :
Fast, private, synchronized and simple messaging.
Mais le produit doit posséder sa propre identité visuelle, son propre modèle métier et son propre code.
12. Objectifs
2.1 Objectifs MVP
Le MVP doit permettre :
1. Création de compte.
2. Connexion.
3. Vérification email.
4. Profil utilisateur.
5. Recherche d'utilisateurs.
6. Conversation privée.
7. Envoi de messages en temps réel.
8. Réception de messages en temps réel.
9. Accusé d'envoi.
10. Accusé de livraison.
11. Accusé de lecture.
12. Typing indicator.
13. Présence online/offline.
14. Envoi d'images.
15. Envoi de fichiers.
16. Réponse à un message.
17. Modification d'un message.
18. Suppression d'un message.
19. Recherche dans une conversation.
20. Création de groupes.
21. Administration des groupes.
22. Notifications.
23. Gestion des sessions.
24. Blocage d'utilisateurs.
25. Paramètres de confidentialité.
3. Fonctionnalités post-MVP
Les fonctionnalités suivantes doivent être prévues architecturalement mais ne sont pas obligatoires dans le
premier MVP :
• appels audio ;
• appels vidéo ;
• partage d'écran ;
• messages vocaux ;
• messages vidéo ;
• stories ;
• réactions avancées ;
2• sondages ;
• messages programmés ;
• messages épinglés ;
• canaux avancés ;
• bots ;
• mini-applications ;
• paiements ;
• traduction automatique ;
• IA conversationnelle ;
• chiffrement de bout en bout ;
• multi-device avancé ;
• desktop application ;
• mobile applications.
4. Personas
4.1 Utilisateur standard
Veut communiquer rapidement avec ses amis, collègues ou famille.
4.2 Administrateur de groupe
Crée et gère une communauté.
4.3 Créateur de canal
Publie du contenu à une audience importante.
4.4 Administrateur plateforme
Gère :
• utilisateurs ;
• modération ;
• signalements ;
• sécurité ;
• statistiques ;
• configuration globale.
35. Principes produit
Performance
Les messages doivent apparaître immédiatement après réception.
Simplicité
Une conversation doit être accessible en très peu d'actions.
Temps réel
Les changements importants doivent être synchronisés sans refresh.
Fiabilité
Un message ne doit pas disparaître silencieusement en cas de problème réseau.
Sécurité
Les données privées doivent être protégées par défaut.
Scalabilité
L'architecture doit permettre de passer progressivement de centaines à des centaines de milliers
d'utilisateurs.
6. Architecture fonctionnelle
WEB APPLICATION
│
│ HTTPS
▼
┌──────────────────┐
│ API / Gateway
│
└────────┬─────────┘
│
┌──────────┴──────────┐
│
REST│
WebSocket
││
4└──────────┬──────────┘
▼
┌──────────────────────┐
│ Messaging Backend│
││
│ Auth│
│ Users
│
│ Conversations│
│ Messages│
│ Groups│
│ Channels│
│ Notifications│
│ Files│
└──────────┬───────────┘
│
┌───────────┼───────────┐
▼
PostgreSQL
▼
Redis
▼
Object Storage
7. Architecture frontend
Le frontend Web doit être construit autour de trois grandes zones.
┌──────────────────────────────────────────────────────────┐
│ Header / Search
│
├───────────────┬──────────────────────────┬───────────────┤
││││
│ Conversation││ Sidebar││ Details│
│ Panel│
│││ Chats│Messages
││
│ Profile│
│ Groups││ Channels│Media│ Members│
Input│ Files│
│││ Settings│
Message Area
└───────────────┴──────────────────────────┴───────────────┘
Le panneau de droite peut être masqué sur les écrans plus petits.
58. Écrans principaux
8.1 Landing page
• présentation ;
• bouton Login ;
• bouton Register ;
• informations produit.
8.2 Login
Champs :
• email ;
• password.
Actions :
• Login ;
• Forgot password ;
• Register.
8.3 Register
Champs :
• username ;
• email ;
• password ;
• confirmation password.
8.4 Verification
• code de vérification ;
• resend code.
8.5 Main Messenger
Structure :
• sidebar ;
• conversation list ;
• conversation view ;
• details panel.
68.6 New Conversation
Recherche :
• username ;
• email ;
• nom.
8.7 Group Creation
• nom ;
• photo ;
• description ;
• membres.
8.8 Channel Creation
• nom ;
• username/public identifier ;
• description ;
• photo ;
• visibilité.
8.9 User Profile
• avatar ;
• username ;
• name ;
• bio ;
• status.
8.10 Settings
Sections :
• Account ;
• Privacy ;
• Notifications ;
• Sessions ;
• Appearance ;
• Language ;
• Security.
79. Navigation
Navigation principale :
Messenger
├── Chats
├── Groups
├── Channels
├── Contacts
├── Saved Messages
└── Settings
Sur Desktop, la navigation est principalement orientée sidebar.
Sur mobile/tablette Web, elle devient responsive.
10. Conversation
Une conversation doit afficher :
• avatar ;
• nom ;
• online/offline ;
• last seen ;
• messages ;
• timestamps ;
• read state ;
• reply previews ;
• attachments ;
• reactions ;
• message actions.
11. Types de conversations
Le système doit distinguer :
PRIVATE
Conversation entre deux utilisateurs.
8GROUP
Conversation multi-utilisateurs.
CHANNEL
Communication principalement unidirectionnelle :
ADMIN → SUBSCRIBERS
SAVED_MESSAGES
Conversation personnelle permettant à l'utilisateur de sauvegarder des messages/fichiers.
12. Types de messages
MVP :
TEXT
IMAGE
VIDEO
DOCUMENT
AUDIO
FILE
SYSTEM
Post-MVP :
VOICE
VIDEO_NOTE
LOCATION
CONTACT
POLL
STICKER
GIF
13. Message lifecycle
Un message doit suivre :
9CREATED
↓
SENT
↓
DELIVERED
↓
READ
En cas d'erreur :
CREATED
↓
FAILED
Le frontend doit permettre :
Retry
14. Typing indicator
Lorsqu'un utilisateur commence à écrire :
Client A
│
│ typing.start
▼
WebSocket
│
▼
Client B
Le serveur ne doit pas enregistrer chaque événement de frappe en base.
Redis ou une mémoire temporaire doit être utilisée.
15. Présence utilisateur
États :
10ONLINE
OFFLINE
AWAY
Informations possibles :
• last_seen_at ;
• current session ;
• active device.
La présence doit être temporaire et principalement gérée par Redis.
PostgreSQL conserve seulement les informations nécessaires à l'historique.
16. Notifications
Notifications :
• nouveau message ;
• mention ;
• réponse ;
• ajout à un groupe ;
• ajout à un canal ;
• message système ;
• connexion sur nouveau device.
17. Recherche
La recherche doit permettre :
• utilisateurs ;
• conversations ;
• messages ;
• groupes ;
• canaux.
MVP :
PostgreSQL Full Text Search.
Évolution :
11Elasticsearch/OpenSearch si le volume devient important.
18. Partage de fichiers
Les fichiers ne doivent PAS être stockés directement dans PostgreSQL.
Architecture :
Browser
│
│ Request upload URL
▼
Backend
│
│ Presigned URL
▼
Object Storage
│
▼
Backend
│
▼
Message + Attachment
Le message stocke uniquement les métadonnées.
19. Gestion des fichiers
Métadonnées :
• filename ;
• MIME type ;
• size ;
• storage key ;
• URL ;
• checksum ;
• uploader ;
• created_at.
Restrictions :
• taille maximale configurable ;
12• MIME whitelist ;
• antivirus scanning recommandé ;
• validation extension + MIME ;
• checksum.
20. Groupes
Un groupe possède :
• owner ;
• admins ;
• members ;
• permissions ;
• avatar ;
• description.
Permissions :
SEND_MESSAGES
SEND_MEDIA
ADD_MEMBERS
REMOVE_MEMBERS
EDIT_GROUP
DELETE_MESSAGES
PIN_MESSAGES
MANAGE_ADMINS
21. Canaux
Un canal possède :
• owner ;
• admins ;
• subscribers ;
• posts ;
• public/private status.
Types :
13PUBLIC
PRIVATE
Un canal public possède un username unique.
Exemple :
@mychannel
22. Modèle de données
Entités principales
User
Session
Conversation
ConversationMember
Message
MessageAttachment
MessageReaction
MessageRead
Group
Channel
Notification
Block
Report
File
23. Tables PostgreSQL
users
id UUID PK
username VARCHAR UNIQUE
email VARCHAR UNIQUE
password_hash VARCHAR
first_name VARCHAR
last_name VARCHAR
14bio TEXT
avatar_file_id UUID
status VARCHAR
email_verified BOOLEAN
last_seen_at TIMESTAMPTZ
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
deleted_at TIMESTAMPTZ
version BIGINT
user_sessions
id UUID PK
user_id UUID FK users.id
refresh_token_hash VARCHAR
device_name VARCHAR
device_type VARCHAR
ip_address INET
user_agent TEXT
last_active_at TIMESTAMPTZ
created_at TIMESTAMPTZ
expires_at TIMESTAMPTZ
revoked_at TIMESTAMPTZ
Relation :
users 1 ─── N user_sessions
24. conversations
id UUID PK
type VARCHAR
title VARCHAR
username VARCHAR UNIQUE NULL
description TEXT
avatar_file_id UUID
owner_id UUID FK users.id
created_at TIMESTAMPTZ
15updated_at TIMESTAMPTZ
deleted_at TIMESTAMPTZ
Types :
PRIVATE
GROUP
CHANNEL
SAVED
25. conversation_members
conversation_id UUID FK
user_id UUID FK
role VARCHAR
status VARCHAR
joined_at TIMESTAMPTZ
left_at TIMESTAMPTZ
last_read_message_id UUID
muted_until TIMESTAMPTZ
Primary key :
(conversation_id, user_id)
Relation :
conversations N ─── N users
26. messages
id UUID PK
conversation_id UUID FK
sender_id UUID FK
type VARCHAR
body TEXT
reply_to_message_id UUID NULL
16forwarded_from_message_id UUID NULL
edited_at TIMESTAMPTZ NULL
deleted_at TIMESTAMPTZ NULL
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
sequence_number BIGINT
Relation :
conversation 1 ─── N messages
user 1 ─── N messages
message 1 ─── N messages
reply_to
27. message_attachments
id UUID PK
message_id UUID FK
file_id UUID FK
attachment_type VARCHAR
width INTEGER
height INTEGER
duration_seconds INTEGER
thumbnail_file_id UUID
created_at TIMESTAMPTZ
Relation :
message 1 ─── N attachments
28. files
id UUID PK
owner_id UUID FK users.id
storage_provider VARCHAR
storage_key VARCHAR
17original_name VARCHAR
mime_type VARCHAR
size_bytes BIGINT
checksum VARCHAR
status VARCHAR
created_at TIMESTAMPTZ
deleted_at TIMESTAMPTZ
29. message_reactions
id UUID PK
message_id UUID FK
user_id UUID FK
reaction VARCHAR
created_at TIMESTAMPTZ
Contrainte :
UNIQUE(message_id, user_id, reaction)
30. message_reads
message_id UUID FK
user_id UUID FK
read_at TIMESTAMPTZ
Pour optimiser les performances, on peut préférer dans un premier temps :
conversation_members.last_read_message_id
plutôt qu'une ligne par message lu.
1831. groups
conversation_id UUID PK/FK
max_members BIGINT
join_policy VARCHAR
approval_required BOOLEAN
created_at TIMESTAMPTZ
32. channels
conversation_id UUID PK/FK
visibility VARCHAR
post_permission VARCHAR
subscriber_count BIGINT
created_at TIMESTAMPTZ
33. notifications
id UUID PK
user_id UUID FK
type VARCHAR
title VARCHAR
body TEXT
data JSONB
read_at TIMESTAMPTZ
created_at TIMESTAMPTZ
34. notification_preferences
user_id UUID PK/FK
message_notifications BOOLEAN
group_notifications BOOLEAN
channel_notifications BOOLEAN
mention_notifications BOOLEAN
sound_enabled BOOLEAN
19email_notifications BOOLEAN
updated_at TIMESTAMPTZ
35. blocked_users
user_id UUID FK
blocked_user_id UUID FK
created_at TIMESTAMPTZ
Primary key :
(user_id, blocked_user_id)
36. reports
id UUID PK
reporter_id UUID FK
reported_user_id UUID FK NULL
conversation_id UUID FK NULL
message_id UUID FK NULL
reason VARCHAR
description TEXT
status VARCHAR
reviewed_by UUID FK users.id
created_at TIMESTAMPTZ
resolved_at TIMESTAMPTZ
37. Audit logs
id UUID PK
actor_id UUID FK
action VARCHAR
entity_type VARCHAR
entity_id UUID
metadata JSONB
20ip_address INET
created_at TIMESTAMPTZ
Les actions sensibles doivent être journalisées.
38. Relations principales
USER
│
├────< USER_SESSION
│
├────< MESSAGE
│
├────< CONVERSATION_MEMBER >──── CONVERSATION
│
├────< NOTIFICATION
│
├────< FILE
│
├────< BLOCKED_USER
│
└────< REPORT
CONVERSATION
│
├────< CONVERSATION_MEMBER
│
├────< MESSAGE
│
├──── GROUP
│
└──── CHANNEL
MESSAGE
│
├────< MESSAGE_ATTACHMENT >──── FILE
│
├────< MESSAGE_REACTION
│
21└────< MESSAGE
└── reply_to_message
39. API REST
Base URL :
/api/v1
Authentication
POST /auth/register
POST /auth/login
POST /auth/logout
POST /auth/refresh
POST /auth/verify-email
POST /auth/forgot-password
POST /auth/reset-password
40. User APIs
GET
/users/me
PATCH /users/me
GET
/users/{id}
GET
/users/search?q=
POST
/users/{id}/block
DELETE /users/{id}/block
41. Conversation APIs
GET
POST
GET
/conversations
/conversations/private
/conversations/{id}
22PATCH /conversations/{id}
DELETE /conversations/{id}
42. Message APIs
GET
/conversations/{id}/messages
POST
/conversations/{id}/messages
PATCH /messages/{id}
DELETE /messages/{id}
POST
/messages/{id}/forward
POST
/messages/{id}/reaction
DELETE /messages/{id}/reaction
43. Group APIs
POST
/groups
GET
/groups/{id}
PATCH /groups/{id}
DELETE /groups/{id}
POST
/groups/{id}/members
DELETE /groups/{id}/members/{userId}
POST
/groups/{id}/admins
DELETE /groups/{id}/admins/{userId}
44. Channel APIs
POST
/channels
GET
/channels/{id}
PATCH /channels/{id}
DELETE /channels/{id}
POST
/channels/{id}/subscribe
DELETE /channels/{id}/subscribe
23POST
/channels/{id}/messages
45. File APIs
POST /files/upload-url
POST /files/{id}/complete
GET /files/{id}
DELETE /files/{id}
Le frontend ne doit pas envoyer les gros fichiers directement via le serveur applicatif si un stockage objet
avec URL pré-signée est disponible.
46. WebSocket
Endpoint :
/ws
Après authentification :
/ws?token=ACCESS_TOKEN
Il est préférable, lorsque possible, de transmettre l'identité via un mécanisme de handshake sécurisé plutôt
que d'exposer durablement le token dans une URL.
47. WebSocket events
Client → Server
message.send
message.edit
message.delete
typing.start
typing.stop
24conversation.read
reaction.add
reaction.remove
presence.update
48. Server → Client
message.created
message.updated
message.deleted
message.delivered
message.read
typing.started
typing.stopped
presence.changed
reaction.added
reaction.removed
conversation.updated
notification.created
49. Exemple message.send
{
"event": "message.send",
"requestId": "uuid",
"conversationId": "uuid",
"clientMessageId": "uuid",
"type": "TEXT",
"body": "Bonjour"
}
25Réponse :
{
"event": "message.created",
"requestId": "uuid",
"message": {
"id": "uuid",
"conversationId": "uuid",
"senderId": "uuid",
"body": "Bonjour",
"createdAt": "timestamp"
}
}
50. Pourquoi REST + WebSocket
REST est utilisé pour :
• authentification ;
• chargement initial ;
• recherche ;
• pagination ;
• profil ;
• paramètres ;
• historique.
WebSocket est utilisé pour :
• nouveaux messages ;
• typing ;
• présence ;
• lecture ;
• réactions ;
• changements instantanés.
Il ne faut pas essayer de faire toute l'application uniquement avec WebSocket.
51. Flux complet d'envoi d'un message
User
│
26│ Click Send
▼
Frontend
│
│ WebSocket message.send
▼
WebSocket Gateway
│
▼
Message Service
│
├── Validate user
├── Validate membership
├── Validate conversation
├── Persist message
│
▼
PostgreSQL
│
▼
Publish MessageCreatedEvent
│
├───────────────┐
▼
Redis
▼
Notification
││
▼
WebSocket▼
Push/Email
│
▼
Recipients
52. Offline / reconnexion
Le client doit gérer :
CONNECTED
DISCONNECTED
RECONNECTING
Lors d'une déconnexion :
1. les messages non confirmés restent temporairement en mémoire ;
272. le frontend tente une reconnexion ;
3. le serveur renvoie les événements manquants ;
4. les messages sont réconciliés grâce au clientMessageId .
53. Idempotence
Chaque message envoyé depuis le client doit posséder :
client_message_id
Le backend doit empêcher la création de doublons.
Exemple :
clientMessageId = abc123
Si le client renvoie le même message après reconnexion :
Server → existing message
et non :
Server → second message
54. Pagination
Les messages doivent être chargés par curseur.
Préférer :
GET /messages?before=<messageId>&limit=50
plutôt que :
28?page=10000
Cela permet de mieux supporter les conversations importantes.
55. Stratégie Redis
Redis doit gérer :
• présence ;
• typing indicators ;
• WebSocket session mapping ;
• rate limiting ;
• cache ;
• distributed locks ;
• éventuellement pub/sub.
Exemple :
user:{id}:presence
user:{id}:connections
conversation:{id}:online
rate-limit:{ip}
56. Synchronisation multi-onglets
Un même utilisateur peut ouvrir :
Chrome
Firefox
Mobile browser
Desktop
Chaque connexion WebSocket est identifiée.
User
├── Session A
29├── Session B
└── Session C
Lorsqu'un message arrive :
Backend
↓
All active sessions of user
57. Authentification
Recommandation :
Access Token
+
Refresh Token
Access token court :
10–15 minutes
Refresh token :
longue durée
Les refresh tokens doivent être stockés sous forme hashée côté serveur.
58. Sécurité
Obligatoire :
• HTTPS ;
• password hashing ;
• rate limiting ;
• validation serveur ;
• CORS strict ;
• protection brute-force ;
30• validation MIME ;
• antivirus fichiers ;
• SQL injection protection ;
• XSS protection ;
• CSRF selon stratégie d'authentification ;
• sécurité WebSocket ;
• audit logs ;
• session revocation.
59. Autorisation
RBAC :
USER
ADMIN
SUPER_ADMIN
MODERATOR
Pour les groupes :
OWNER
ADMIN
MEMBER
Pour les canaux :
OWNER
ADMIN
SUBSCRIBER
60. Architecture backend recommandée
Pour le MVP :
Modular Monolith
Modules :
31auth
users
sessions
conversations
messages
groups
channels
files
notifications
search
moderation
admin
common
Chaque module doit avoir :
domain
application
infrastructure
presentation
Exemple :
messages/
├── domain/
├── application/
├── infrastructure/
└── presentation/
61. Pourquoi ne pas commencer avec des
microservices
Un système de messagerie est déjà complexe.
Commencer avec :
10 microservices
Kafka
API Gateway
32Service Discovery
multiple databases
ajouterait beaucoup de complexité inutile.
Le Modular Monolith permet de garder :
• séparation métier ;
• testabilité ;
• évolutivité ;
• simplicité de déploiement.
Les modules pourront ensuite devenir des services indépendants si nécessaire.
62. Architecture future
Lorsque le volume augmente :
Load Balancer
│
┌────────────┴────────────┐
▼
API Instance 1
▼
API Instance 2
│
│
└──────────┬──────────────┘
▼
Redis
│
┌────────┴────────┐
▼
PostgreSQL
▼
Message Broker
│
┌──────────────┼─────────────┐
▼
Messaging
▼
Notification
▼
Media
3363. Base de données : indexes essentiels
users
UNIQUE(email)
UNIQUE(username)
INDEX(status)
messages
INDEX(conversation_id, created_at DESC)
INDEX(sender_id, created_at DESC)
INDEX(reply_to_message_id)
conversation_members
INDEX(user_id)
INDEX(conversation_id)
notifications
INDEX(user_id, created_at DESC)
INDEX(user_id, read_at)
64. Soft delete
Les messages supprimés ne doivent pas forcément être physiquement supprimés immédiatement.
Exemple :
deleted_at
Le frontend affiche :
Message supprimé
34Une politique de purge peut être appliquée plus tard.
65. Message editing
Un message peut être modifié uniquement selon les règles définies :
sender_id == current_user
Le système conserve :
edited_at
Pour un système avancé, une table :
message_edits
peut conserver l'historique.
66. Suppression
Deux types :
Delete for me
Le message disparaît uniquement pour l'utilisateur.
Delete for everyone
Le message est supprimé pour tous selon les permissions et règles produit.
Pour cela, prévoir éventuellement :
message_visibility
message_deletions
3567. Read receipts
Pour éviter une explosion du nombre de lignes :
conversation_members.last_read_message_id
est recommandé pour le MVP.
Le serveur peut déterminer :
message.id <= last_read_message_id
68. Notifications push
Architecture :
MessageCreatedEvent
│
▼
Notification Service
│
├── WebSocket
├── Browser Push
└── Email (optionnel)
Les notifications Web Push doivent être traitées comme un canal séparé du WebSocket.
69. Browser Push
Prévoir une table :
push_subscriptions
id UUID
user_id UUID
endpoint TEXT
36p256dh_key TEXT
auth_key TEXT
device_info JSONB
created_at TIMESTAMPTZ
revoked_at TIMESTAMPTZ
70. Recherche
MVP :
PostgreSQL
+
GIN indexes
+
Full Text Search
Recherche :
"meeting tomorrow"
doit pouvoir retrouver les messages concernés.
Phase avancée :
OpenSearch
71. Cache
Ne jamais considérer Redis comme la source de vérité.
Source de vérité :
PostgreSQL
Redis :
37performance layer
72. Transactions
Exemple :
Création d'un groupe :
BEGIN
create conversation
create group
create owner membership
create members
COMMIT
Si une étape échoue :
ROLLBACK
73. Domain Events
Événements principaux :
UserRegistered
UserLoggedIn
ConversationCreated
MemberAdded
MemberRemoved
MessageCreated
MessageEdited
MessageDeleted
MessageRead
GroupCreated
ChannelCreated
FileUploaded
38NotificationCreated
UserBlocked
74. Observabilité
Le système doit produire :
Logs
• authentication ;
• WebSocket ;
• messages ;
• erreurs ;
• fichiers ;
• sécurité.
Metrics
• active users ;
• WebSocket connections ;
• messages/sec ;
• latency ;
• error rate ;
• DB latency ;
• Redis latency ;
• upload rate.
Tracing
Pour suivre :
HTTP request
→ service
→ DB
→ Redis
→ event
75. Monitoring critique
Alertes :
39API error rate > threshold
WebSocket disconnect rate > threshold
DB CPU high
DB connections high
Redis memory high
Message processing latency high
Storage errors
Authentication failures
76. Performance targets MVP
Objectifs techniques :
API
P95 :
< 300 ms
pour les opérations classiques.
WebSocket
Réception d'un événement :
< 500 ms
dans des conditions normales.
Database
Les requêtes critiques :
< 100 ms
objectif.
4077. Scalabilité
Architecture initiale prévue pour :
1 000 utilisateurs simultanés
avec possibilité d'évolution vers :
10 000+
100 000+
grâce à :
• stateless API ;
• Redis ;
• PostgreSQL optimisé ;
• WebSocket horizontal scaling ;
• object storage ;
• load balancing.
78. Tests
Unit tests
Tester :
• message creation ;
• permissions ;
• group rules ;
• channel rules ;
• message deletion ;
• reactions.
Integration tests
Tester :
API
+
PostgreSQL
41+
Redis
+
WebSocket
End-to-end
Scénario :
User A login
↓
User B login
↓
A creates conversation
↓
A sends message
↓
B receives message
↓
B reads message
↓
A receives read receipt
79. CI/CD
Pipeline :
Git Push
↓
Lint
↓
Unit Tests
↓
Integration Tests
↓
Build
↓
Security Scan
↓
Docker Image
↓
Deploy Staging
42↓
Smoke Tests
↓
Production
80. Environnements
Prévoir :
development
staging
production
Chaque environnement doit avoir :
• database indépendante ;
• Redis indépendant ;
• storage indépendant ;
• secrets indépendants.
81. Variables de configuration
Exemple :
DATABASE_URL
REDIS_URL
JWT_SECRET
JWT_ISSUER
STORAGE_BUCKET
STORAGE_ACCESS_KEY
STORAGE_SECRET_KEY
WEBSOCKET_ALLOWED_ORIGINS
MAIL_PROVIDER_API_KEY
Aucun secret dans Git.
4382. API versioning
Toutes les API publiques doivent être versionnées :
/api/v1/...
Une future version pourra être :
/api/v2/...
83. Erreurs API
Format standard :
{
"timestamp": "2026-08-18T20:00:00Z",
"status": 400,
"code": "MESSAGE_TOO_LONG",
"message": "Message exceeds maximum allowed length.",
"requestId": "uuid"
}
84. Règles métier importantes
Message
Un utilisateur ne peut envoyer un message que s'il possède les permissions nécessaires dans la
conversation.
Private chat
Les deux utilisateurs doivent être autorisés à communiquer.
44Group
L'administrateur peut :
• ajouter ;
• retirer ;
• promouvoir ;
• rétrograder.
Channel
Seuls les utilisateurs disposant du droit de publication peuvent créer des posts.
85. Limites MVP
Configurer les limites côté backend.
Exemple :
Max message length
Max attachment size
Max group members
Max username length
Max avatar size
Max files per message
Max messages per second
Ces valeurs doivent être configurables, pas hardcodées partout.
86. Abuse prevention
Prévoir :
Rate limiting
Spam detection
User blocking
User reporting
Message reporting
Admin moderation
45Account suspension
IP throttling
87. Admin Dashboard
L'administration Web doit permettre :
Dashboard
Users
Groups
Channels
Messages
Reports
Moderation
Files
Sessions
System health
Audit logs
KPIs :
Total users
Active users
Online users
Messages/day
Messages/hour
Groups
Channels
Reports
Blocked accounts
Storage usage
WebSocket connections
88. MVP vs Post-MVP
MVP
Authentication
Profiles
46Private chat
Groups
Text messages
Images
Files
Read receipts
Typing
Presence
Notifications
Search
Block
Sessions
Admin
V2
Voice messages
Reactions
Polls
Pinned messages
Advanced search
Channels
Browser push
Message scheduling
V3
Audio calls
Video calls
Screen sharing
Stories
Bots
AI
E2EE
Payments
Mini apps
4789. Definition of Done
Une fonctionnalité est considérée terminée uniquement lorsque :
• frontend terminé ;
• backend terminé ;
• API documentée ;
• validation serveur implémentée ;
• permissions implémentées ;
• tests écrits ;
• gestion des erreurs terminée ;
• logs ajoutés ;
• métriques ajoutées si nécessaire ;
• responsive vérifié ;
• sécurité vérifiée ;
• documentation mise à jour.
90. Critères de réussite du MVP
Le MVP doit permettre à deux utilisateurs réels de :
1. créer un compte ;
2. se connecter ;
3. rechercher un utilisateur ;
4. commencer une conversation ;
5. envoyer un message ;
6. recevoir le message instantanément ;
7. voir le statut de livraison ;
8. voir le statut de lecture ;
9. voir quand l'autre écrit ;
10. envoyer une image ;
11. créer un groupe ;
12. ajouter des membres ;
13. gérer le groupe ;
14. rechercher des messages ;
15. se déconnecter ;
16. reconnecter leur compte depuis un autre navigateur.
Le système doit fonctionner sans refresh manuel.
4891. Architecture cible finale
┌─────────────────────┐
│
WEB CLIENT│
│ React / Next.js│
└──────────┬──────────┘
│
HTTPS / WebSocket
│
┌─────────────────▼─────────────────┐
│
API / WS GATEWAY
│
└─────────────────┬─────────────────┘
│
┌─────────────────────────┼─────────────────────────┐
││▼
AUTH MODULE▼
CHAT MODULE│││┌─────┴─────┐│
│││
▼
USERS
│
▼
MEDIA MODULE
│
▼
MESSAGES
│
▼
GROUPS
││
▼
CHANNELS▼
MEMBERS
│
┌─────────┴─────────┐
▼
PostgreSQL▼
Redis
││
└─────────┬─────────┘
▼
Notification System
│
┌─────────┴─────────┐
▼
WebSocket
▼
Web Push
49
▼
STORAGE92. Stack technique recommandé
Frontend
Next.js
React
TypeScript
Tailwind CSS
TanStack Query
Zustand
WebSocket client
Backend
Java 21
Spring Boot
Spring Security
Spring Data JPA
WebSocket / STOMP ou WebSocket natif
Flyway
Data
PostgreSQL
Redis
Storage
Amazon S3
ou S3-compatible storage
Infrastructure
Docker
GitHub Actions
AWS
50Monitoring
Sentry
Prometheus
Grafana
CloudWatch
93. Décision architecturale finale
Pour le premier lancement, la recommandation est :
Next.js
+
Spring Boot Modular Monolith
+
PostgreSQL
+
Redis
+
WebSocket
+
S3
Cette combinaison donne une architecture suffisamment simple pour être développée rapidement tout en
étant suffisamment robuste pour évoluer vers une véritable plateforme de messagerie.
94. Principe fondamental
Le produit doit être conçu dès le début autour de cette règle :
PostgreSQL conserve l'état durable. Redis accélère le système. WebSocket transporte le
temps réel. Object Storage conserve les fichiers. Le frontend ne fait jamais confiance à
son propre état comme source de vérité.
C'est cette séparation qui permettra d'éviter une grande partie des problèmes classiques des applications
de messagerie.
51