/** @type {import('next').NextConfig} */
const nextConfig = {
  async redirects() {
    return [
      {
        source: '/',
        destination: '/kronos',
        permanent: true,
      },
    ];
  },
};

module.exports = nextConfig;