# Class Manager Frontend

Vue 3 + TypeScript + Vite frontend for the information technology classroom management system.

## Stack

- Vue 3 + `<script setup>`
- TypeScript
- Vue Router + Pinia
- Naive UI
- Axios
- ECharts
- Markdown-It + DOMPurify
- Monaco Editor, lazy loaded by `MarkdownEditor.vue`

## Development

The backend is expected to run at `http://localhost:8080`, normally through Docker Compose from `../backend`.

```bash
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

## Useful Scripts

```bash
npm run test
npm run check
npm run build
npm run preview
```

`npm run check` runs type checking, unit tests, and a production build. `npm run build` runs `vue-tsc -b` before Vite production build.

## Notes

- API wrappers live in `src/api`.
- Shared backend types live in `src/types/api.ts`.
- Task/exam question schema helpers live in `src/types/taskSchema.ts`.
- Markdown editing uses Monaco only when the editor component is opened; normal dashboard and list pages should not pull Monaco into the initial route.
- `vite.config.ts` manually chunks Monaco, ECharts, Naive UI, and Vue-related vendors to keep build warnings manageable.

## Current Main Routes

- Admin: `/admin/overview`, `/admin/classes`, `/admin/teachers`, `/admin/students`
- Teacher: `/teacher/home`, `/teacher/courses`, `/teacher/tasks`, `/teacher/exams`, `/teacher/projects`, `/teacher/students`, `/teacher/grade-export`
- Student: `/student/home`, `/student/courses`, `/student/tasks/:taskId`, `/student/exams`, `/student/projects`, `/student/evaluation`, `/student/drive`
