import { api } from '@/lib/api'

export default async function Test() {
  try {
    const data = await api.getCourses()
    console.log("SERVER FETCH SUCCESS:", data.content.length)
  } catch (e) {
    console.error("SERVER FETCH FAIL:", e)
  }
  return <div>Test</div>
}
