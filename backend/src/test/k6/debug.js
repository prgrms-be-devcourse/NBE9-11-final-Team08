import http from 'k6/http';

export default function () {
    // const signupRes = http.post(
    //     'http://localhost:8080/api/auth/signup',
    //     JSON.stringify({
    //         "email": "user@example.com",
    //         "password": "testtest",
    //         "nickname": "test",
    //         "profileImage": "string",
    //         "userRole": "user"
    //     }),
    //     {
    //         headers: {
    //             'Content-Type': 'application/json'
    //         }
    //     }
    // );
    //
    //
    // console.log('LOGIN STATUS = ' + signupRes.status);
    // console.log('LOGIN BODY = ' + signupRes.body);
    //
    // if (signupRes.status !== 200) {
    //     console.error('회원가입 실패');
    //     return;
    // }

    const loginRes = http.post(
        'http://localhost:8080/api/auth/login',
        JSON.stringify({
            email: 'user1@playlearn.com',
            password: 'user1234!'
        }),
        {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    );

    console.log('LOGIN STATUS = ' + loginRes.status);
    console.log('LOGIN BODY = ' + loginRes.body);

    if (loginRes.status !== 200) {
        console.error('로그인 실패');
        return;
    }

    const token = JSON.parse(loginRes.body).accessToken;

    const payload = {
        courseId: 1,
        chapterId: 1,
        lectureId: 1,
        eventType: 'LECTURE_ENTER',
        eventTime: new Date().toISOString(),
        eventKey: 'debug-test'
    };

    const res = http.post(
        'http://localhost:8080/api/learning-events',
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        }
    );

    console.log('EVENT STATUS = ' + res.status);
    console.log('EVENT BODY = ' + res.body);
}