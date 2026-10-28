
// JWT와 로그인 사용자 정보는 메모리에만 저장
let session = { accessToken: "", user: null };

export const getSession = () => session;

// 로그아웃 시 로그인 정보 삭제
export function clearSession() {
  session = { accessToken: "", user: null };
}

// FE-02: JWT를 Bearer 헤더에 추가
function authorizationHeaders() {
  if (session.accessToken) {
    return {
      Authorization: `Bearer ${session.accessToken}`,
    };
  }

  return {};
}

// 공통 API 요청 함수
export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
  });

  const data = response.status === 204 ? null : await response.json();

  if (!response.ok) {
    // FE-03: 401 응답 시 로그인 상태 초기화
    if (response.status === 401) {
      clearSession();
    }

    const error = new Error(
      data?.message || data?.code || `HTTP ${response.status}`
    );

    error.status = response.status;
    error.code = data?.code;

    throw error;
  }

  return data;
}

// FE-01: 로그인 및 JWT 저장
export async function login(email, password) {
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  session = {
    accessToken: data.accessToken,
    user: data.user,
  };

  return data.user;
}

// FE-04: 게시글 작성자 권한 확인
export function canEdit(post) {
  if (!session.user || !post) {
    return false;
  }

  return session.user.id === post.authorId;
}

// 게시판 API
export const api = {
  list: () => request("/posts"),

  detail: (id) => request(`/posts/${id}`),

  create: (input) =>
    request("/posts", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  update: (id, input) =>
    request(`/posts/${id}`, {
      method: "PUT",
      body: JSON.stringify(input),
    }),

  remove: (id) =>
    request(`/posts/${id}`, {
      method: "DELETE",
    }),

  me: () => request("/me"),
};
