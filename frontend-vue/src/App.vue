
<script setup>
import { ref, onMounted } from "vue";
import { api, login, getSession, clearSession, canEdit } from "./api";

const posts = ref([]),
  selected = ref(null),
  title = ref(""),
  content = ref(""),
  editing = ref(null);

const email = ref("alice@class.test"),
  password = ref("Classroom!2026"),
  user = ref(null),
  busy = ref(false),
  message = ref("데이트 장소를 불러오는 중입니다.");

const refresh = async () => {
  posts.value = await api.list();
};

async function run(action, success) {
  busy.value = true;
  try {
    await action();
    message.value = success;
  } catch (e) {
    if (e.status === 401) {
      message.value = "로그인이 만료되었거나 인증에 실패했습니다. 다시 로그인해 주세요.";
    } else {
      message.value = e.message;
    }
  } finally {
    user.value = getSession().user;
    busy.value = false;
  }
}

onMounted(() => run(refresh, "데이트 장소 목록을 불러왔습니다."));

function reset() {
  title.value = "";
  content.value = "";
  editing.value = null;
}

function save() {
  if (!user.value) {
    message.value = "게시글을 작성하려면 먼저 로그인해 주세요.";
    return;
  }

  run(async () => {
    const input = { title: title.value, content: content.value };
    selected.value = editing.value
      ? await api.update(editing.value, input)
      : await api.create(input);
    reset();
    await refresh();
  }, "게시글을 저장했습니다.");
}

function edit() {
  editing.value = selected.value.id;
  title.value = selected.value.title;
  content.value = selected.value.content;
}

function remove() {
  if (confirm("이 데이트 장소 후기를 삭제할까요?")) {
    run(async () => {
      await api.remove(selected.value.id);
      selected.value = null;
      reset();
      await refresh();
    }, "게시글을 삭제했습니다.");
  }
}

function logout() {
  clearSession();
  user.value = null;
  message.value = "로그아웃했습니다.";
}

function me() {
  run(async () => {
    const u = await api.me();
    alert(`${u.name} (${u.email})`);
  }, "내 정보 조회를 완료했습니다.");
}
</script>

<template>
  <main>
    <header>
      <span class="eyebrow">DATEPICK · DATE PLACE REVIEW</span>
      <h1>DatePick 💗</h1>
      <p class="muted">
        대학생 커플을 위한 데이트 장소 추천 및 후기 공유 게시판
      </p>
    </header>

    <p class="banner">
      오늘은 어디서 데이트할까? 💕
      마음에 들었던 데이트 장소와 방문 후기를 공유해 보세요!
    </p>

    <div role="status" class="message">{{ message }}</div>

    <div class="grid">
      <aside>
        <section class="card">
          <h2>로그인</h2>
          <p>{{ user ? `${user.name}님 로그인 중` : "로그아웃 상태" }}</p>

          <form
            @submit.prevent="
              run(() => login(email, password), '로그인했습니다.')
            "
          >
            <label>
              이메일
              <input type="email" v-model="email" required />
            </label>

            <label>
              비밀번호
              <input type="password" v-model="password" required />
            </label>

            <div class="actions">
              <button :disabled="busy">로그인</button>

              <button
                type="button"
                class="secondary"
                @click="logout"
              >
                로그아웃
              </button>
            </div>
          </form>

          <p>
            <small>
              테스트 계정: alice@class.test / bob@class.test
              <br />
              공통 비밀번호: Classroom!2026
            </small>
          </p>

          <button
            class="secondary"
            :disabled="busy"
            @click="me"
          >
            내 정보 조회
          </button>
        </section>

        <section class="card">
          <h2>
            {{ editing ? "데이트 후기 수정" : "데이트 장소 추천하기" }}
          </h2>

          <form @submit.prevent="save">
            <label>
              제목
              <input
                maxlength="100"
                v-model="title"
                placeholder="예: 전주 한옥마을 데이트 추천"
                required
              />
            </label>

            <label>
              내용
              <textarea
                maxlength="5000"
                v-model="content"
                placeholder="데이트 장소와 방문 후기를 작성해 주세요."
                required
              />
            </label>

            <div class="actions">
              <button :disabled="busy">
                {{ editing ? "수정 저장" : "등록" }}
              </button>

              <button
                type="button"
                class="secondary"
                @click="reset"
              >
                입력 초기화
              </button>
            </div>
          </form>
        </section>
      </aside>

      <div>
        <section class="card">
          <h2>
            데이트 장소 목록 <small>{{ posts.length }}개</small>
          </h2>

          <p v-if="!posts.length">
            아직 등록된 데이트 장소가 없어요.
            첫 번째 장소를 추천해 주세요!
          </p>

          <article
            class="post"
            v-for="p in posts"
            :key="p.id"
          >
            <button
              class="post-title"
              :disabled="busy"
              @click="
                run(
                  async () => (selected = await api.detail(p.id)),
                  '게시글을 불러왔습니다.',
                )
              "
            >
              {{ p.title }}
            </button>

            <p>
              <small>
                {{ p.authorName }} ·
                {{ new Date(p.createdAt).toLocaleString() }}
              </small>
            </p>
          </article>
        </section>

        <section class="card">
          <h2>데이트 장소 후기</h2>

          <template v-if="selected">
            <h3>{{ selected.title }}</h3>

            <p class="content">{{ selected.content }}</p>

            <p>
              <small>작성자 {{ selected.authorName }}</small>
            </p>

            <div class="actions" v-if="canEdit(selected)">
              <button class="secondary" @click="edit">
                수정
              </button>

              <button
                class="danger"
                :disabled="busy"
                @click="remove"
              >
                삭제
              </button>
            </div>
          </template>

          <p class="muted" v-else>
            목록에서 데이트 장소를 선택하세요.
          </p>
        </section>
      </div>
    </div>
  </main>
</template>
